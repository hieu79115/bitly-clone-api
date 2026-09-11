package com.thinh.shortener.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlResponseDto {
    private Long id;
    private String originalUrl;
    private String shortUrl; // This will be in the format dpmain/{shortCode}
    private String shortCode;
    private Integer clickCount;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private Set<TagResponseDto> tags;
}
