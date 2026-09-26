package com.thinh.shortener.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopUrlResponseDto {
    private Long id;
    private String title;
    private String shortCode;
    private String shortUrl;
    private String originalUrl;
    private Integer clickCount;
}
