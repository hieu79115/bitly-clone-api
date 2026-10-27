package com.thinh.shortener.domain.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponseDto {
    private Long id;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String bio;
    private String company;
    private String phoneNumber;
    private Boolean isActive;
    private List<String> roles;
    private long totalUrls;
    private long totalClicks;
    private LocalDateTime createdAt;
}
