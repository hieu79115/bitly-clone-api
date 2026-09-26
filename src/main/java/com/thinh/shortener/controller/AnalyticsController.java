package com.thinh.shortener.controller;

import com.thinh.shortener.domain.dto.response.AnalyticsSummaryResponseDto;
import com.thinh.shortener.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/overview")
    public ResponseEntity<AnalyticsSummaryResponseDto> getOverviewAnalytics(
            @RequestParam(required = false, defaultValue = "7") Integer days,
            Principal principal
    ) {
        AnalyticsSummaryResponseDto response = analyticsService.getOverviewAnalytics(days, principal.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<AnalyticsSummaryResponseDto> getAnalytics(
            @PathVariable String shortCode,
            @RequestParam(required = false, defaultValue = "7") Integer days,
            Principal principal
    ) {
        AnalyticsSummaryResponseDto response = analyticsService.getAnalytics(shortCode, days, principal.getName());
        return ResponseEntity.ok(response);
    }
}
