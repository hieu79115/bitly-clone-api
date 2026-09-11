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
public class TagResponseDto {
    private Long id;
    private String name;
    private LocalDateTime createdAt;
}
