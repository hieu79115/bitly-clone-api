package com.thinh.shortener.service;

import com.thinh.shortener.domain.dto.response.AnalyticsSummaryResponseDto;
import com.thinh.shortener.domain.entity.Url;
import jakarta.servlet.http.HttpServletRequest;

public interface AnalyticsService {
    void recordClick(String shortCode, HttpServletRequest request);
    AnalyticsSummaryResponseDto getAnalytics(String shortCode, String email);
}
