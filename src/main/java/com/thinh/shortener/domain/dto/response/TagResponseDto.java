package com.thinh.shortener.domain.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TagResponseDto {
    private Long id;
    private String name;
    private LocalDateTime createdAt;
}
