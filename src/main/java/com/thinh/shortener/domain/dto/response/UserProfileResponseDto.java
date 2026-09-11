package com.thinh.shortener.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponseDto {
    private Long id;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String bio;
    private String phoneNumber;
    private String company;

    private long totalUrls;
    private long totalClicks;

    private LocalDateTime createdAt;
}
