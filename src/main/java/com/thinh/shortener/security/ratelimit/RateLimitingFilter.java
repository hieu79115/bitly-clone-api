package com.thinh.shortener.security.ratelimit;

import com.thinh.shortener.config.RateLimitProperties;
import com.thinh.shortener.util.IpAddressUtil;
import com.thinh.shortener.util.RedisKeyConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final RateLimitProperties rateLimitProperties;
    private final IpAddressUtil ipAddressUtil;

    private static final Pattern REDIRECT_PATTERN = Pattern.compile("^/[a-zA-Z0-9_-]+$");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Skip if rate limiting is disabled globally or for CORS preflight (OPTIONS)
        if (!rateLimitProperties.isEnabled() || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String uri = request.getRequestURI();

        // 2. Skip Swagger UI, API docs, and static resources
        if (isExemptPath(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Determine client identifier and tier
        String clientIp = ipAddressUtil.getClientIp(request);
        String userIdentifier = getAuthenticatedUser();

        RateLimitProperties.Tier tier;
        String key;

        if (uri.startsWith("/api/v1/auth/")) {
            // High security tier for login / register to prevent brute-force & bot floods
            tier = rateLimitProperties.getAuth();
            key = RedisKeyConstants.RATE_LIMIT_PREFIX + "auth:" + clientIp;
        } else if ("POST".equalsIgnoreCase(request.getMethod()) && "/api/v1/urls".equals(uri)) {
            // Rate limit URL creation to prevent spam link generation
            tier = rateLimitProperties.getCreateUrl();
            String id = userIdentifier != null ? "user:" + userIdentifier : "ip:" + clientIp;
            key = RedisKeyConstants.RATE_LIMIT_PREFIX + "create-url:" + id;
        } else if (!uri.startsWith("/api/") && REDIRECT_PATTERN.matcher(uri).matches()) {
            // Redirect tier for short URL accesses
            tier = rateLimitProperties.getRedirect();
            key = RedisKeyConstants.RATE_LIMIT_PREFIX + "redirect:" + clientIp;
        } else {
            // General API tier
            tier = rateLimitProperties.getGeneral();
            String id = userIdentifier != null ? "user:" + userIdentifier : "ip:" + clientIp;
            key = RedisKeyConstants.RATE_LIMIT_PREFIX + "general:" + id;
        }

        // 4. Evaluate rate limit in Redis
        RateLimitResult result = rateLimiterService.tryConsume(
                key,
                tier.getCapacity(),
                tier.getDurationSeconds()
        );

        if (!result.allowed()) {
            sendRateLimitResponse(response, result);
            return;
        }

        // 5. Attach informative rate limit headers to successful responses
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));

        filterChain.doFilter(request, response);
    }

    private boolean isExemptPath(String uri) {
        return uri.startsWith("/v3/api-docs") ||
                uri.startsWith("/swagger-ui") ||
                "/swagger-ui.html".equals(uri) ||
                "/favicon.ico".equals(uri) ||
                uri.startsWith("/actuator");
    }

    private String getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return null;
    }

    private void sendRateLimitResponse(HttpServletResponse response, RateLimitResult result) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        response.setHeader("X-RateLimit-Limit", String.valueOf(result.limit()));
        response.setHeader("X-RateLimit-Remaining", "0");
        response.setHeader("X-RateLimit-Reset", String.valueOf(result.retryAfterSeconds()));
        response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));

        String json = String.format(
                "{\"status\":429,\"message\":\"Too many requests. Please slow down and try again in %d seconds.\",\"timestamp\":\"%s\",\"errors\":null}",
                result.retryAfterSeconds(),
                LocalDateTime.now()
        );

        response.getWriter().write(json);
    }
}
