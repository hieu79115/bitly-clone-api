package com.thinh.shortener.domain.dto.response;

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
public class UserProfileResponseDto {
    private Long id;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String bio;
    private String phoneNumber;
    private String company;

    private List<String> roles;
    private List<String> permissions;

    private long totalUrls;
    private long totalClicks;

    private LocalDateTime createdAt;
}
