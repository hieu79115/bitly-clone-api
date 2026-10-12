package com.thinh.shortener.domain.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUrlResponseDto {
    private Long id;
    private String title;
    private String originalUrl;
    private String shortCode;
    private String shortUrl;
    private String userEmail;
    private Long userId;
    private Integer clickCount;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
