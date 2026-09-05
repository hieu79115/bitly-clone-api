package com.thinh.shortener.controller;

import com.thinh.shortener.domain.dto.request.CreateUrlRequestDto;
import com.thinh.shortener.domain.dto.request.UpdateUrlRequestDto;
import com.thinh.shortener.domain.dto.response.UrlResponseDto;
import com.thinh.shortener.service.AnalyticsService;
import com.thinh.shortener.service.UrlService;
import com.thinh.shortener.util.IpAddressUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;
    private final AnalyticsService analyticsService;
    private final IpAddressUtil ipAddressUtil;

    @PostMapping("/api/v1/urls")
    public ResponseEntity<UrlResponseDto> createUrl(@Valid @RequestBody CreateUrlRequestDto request, Principal principal) {
        // The principal object is automatically injected by Spring Security after successful token authentication.
        // principal.getName() corresponds to the email address we embedded in the token during login.
        UrlResponseDto response = urlService.createShortUrl(request, principal.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirectToOriginalUrl(@PathVariable String shortCode, HttpServletRequest request) {
        String originalUrl = urlService.getOriginalUrl(shortCode);

        String ipAddress = ipAddressUtil.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String referer = request.getHeader("Referer");

        analyticsService.recordClick(shortCode, ipAddress, userAgent, referer);

        // Return HTTP Status 302 (FOUND) to instruct the browser to automatically redirect to the original page.
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(originalUrl))
                .build();
    }

    @GetMapping("/api/v1/urls")
    public ResponseEntity<Page<UrlResponseDto>> getUserUrls(
            @ParameterObject  @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Principal principal
    ) {
        Page<UrlResponseDto> response = urlService.getUserUrls(principal.getName(), pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/api/v1/urls/{id}")
    public ResponseEntity<UrlResponseDto> updateUrl(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUrlRequestDto request,
            Principal principal
    ) {
        UrlResponseDto response = urlService.updateUrl(id, request, principal.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/v1/urls/{id}")
    public ResponseEntity<Void> deleteUrl(
            @PathVariable Long id,
            Principal principal
    ) {
        urlService.deleteUrl(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
