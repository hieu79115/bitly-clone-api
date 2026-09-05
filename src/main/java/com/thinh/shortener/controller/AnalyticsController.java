package com.thinh.shortener.controller;

import com.thinh.shortener.domain.dto.response.AnalyticsSummaryResponseDto;
import com.thinh.shortener.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/{shortCode}")
    public ResponseEntity<AnalyticsSummaryResponseDto> getAnalytics(
            @PathVariable String shortCode,
            Principal principal
    ) {
        AnalyticsSummaryResponseDto response = analyticsService.getAnalytics(shortCode, principal.getName());
        return ResponseEntity.ok(response);
    }
}
