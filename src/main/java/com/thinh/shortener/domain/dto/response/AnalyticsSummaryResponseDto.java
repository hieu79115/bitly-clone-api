package com.thinh.shortener.domain.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class AnalyticsSummaryResponseDto {
    private String shortCode;
    private String originalUrl;
    private long totalClicks;
    private List<Map<String, Object>> clicksByBrowser;
    private List<Map<String, Object>> clicksByOs;
    private List<Map<String, Object>> clicksByDevice;
}
