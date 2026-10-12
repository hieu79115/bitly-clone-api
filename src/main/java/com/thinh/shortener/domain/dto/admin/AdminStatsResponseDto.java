package com.thinh.shortener.domain.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsResponseDto {
    private long totalUsers;
    private long activeUsers;
    private long bannedUsers;

    private long totalUrls;
    private long activeUrls;
    private long disabledUrls;

    private long totalClicks;
    private long totalRoles;

    private java.util.List<AdminUserResponseDto> recentUsers;
    private java.util.List<AdminUrlResponseDto> recentUrls;
}
