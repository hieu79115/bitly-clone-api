package com.thinh.shortener.security.ratelimit;

import com.thinh.shortener.config.RateLimitProperties;
import com.thinh.shortener.util.IpAddressUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @Mock
    private RateLimiterService rateLimiterService;

    @Spy
    private RateLimitProperties rateLimitProperties = new RateLimitProperties();

    @Mock
    private IpAddressUtil ipAddressUtil;

    @InjectMocks
    private RateLimitingFilter rateLimitingFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        rateLimitProperties.setEnabled(true);
    }

    @Test
    @DisplayName("OPTIONS requests should bypass rate limiting (CORS preflight)")
    void doFilter_OptionsRequest_BypassesRateLimit() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("OPTIONS");

        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(rateLimiterService);
    }

    @Test
    @DisplayName("Swagger documentation URLs should be exempt from rate limiting")
    void doFilter_SwaggerUrl_BypassesRateLimit() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");

        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(rateLimiterService);
    }

    @Test
    @DisplayName("Requests within rate limit should proceed with X-RateLimit headers")
    void doFilter_WithinRateLimit_Allowed() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(ipAddressUtil.getClientIp(request)).thenReturn("192.168.1.100");
        when(rateLimiterService.tryConsume(anyString(), anyLong(), anyLong()))
                .thenReturn(RateLimitResult.allowed(10, 9));

        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader("X-RateLimit-Limit", "10");
        verify(response).setHeader("X-RateLimit-Remaining", "9");
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Requests exceeding rate limit should return HTTP 429 and stop filter chain")
    void doFilter_ExceedingRateLimit_Returns429() throws ServletException, IOException {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(ipAddressUtil.getClientIp(request)).thenReturn("192.168.1.100");
        when(response.getWriter()).thenReturn(printWriter);
        when(rateLimiterService.tryConsume(anyString(), anyLong(), anyLong()))
                .thenReturn(RateLimitResult.rejected(10, 45));

        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(response).setHeader("Retry-After", "45");
        verify(response).setHeader("X-RateLimit-Remaining", "0");
        verifyNoInteractions(filterChain);

        printWriter.flush();
        assertThat(stringWriter.toString()).contains("Too many requests");
    }
}
