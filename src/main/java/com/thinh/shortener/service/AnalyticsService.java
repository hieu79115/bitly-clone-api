package com.thinh.shortener.service;

import com.thinh.shortener.domain.dto.response.AnalyticsSummaryResponseDto;

public interface AnalyticsService {
    void recordClick(String shortCode, String ipAddress, String userAgent, String referer);

    AnalyticsSummaryResponseDto getAnalytics(String shortCode, String email);
}
