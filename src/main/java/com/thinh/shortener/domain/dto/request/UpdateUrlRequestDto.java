package com.thinh.shortener.domain.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
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
public class UpdateUrlRequestDto {

    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Future(message = "Expiration time must be in the future")
    private LocalDateTime expiresAt;

    private Boolean clearExpiration;

    private Set<Long> tagIds;
}
