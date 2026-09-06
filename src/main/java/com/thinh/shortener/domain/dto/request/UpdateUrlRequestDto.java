package com.thinh.shortener.domain.dto.request;

import jakarta.validation.constraints.Future;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class UpdateUrlRequestDto {

    @Future(message = "Expiration time must be in the future")
    private LocalDateTime expiresAt;

    private Set<Long> tagIds;
}
