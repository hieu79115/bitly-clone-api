package com.thinh.shortener.domain.dto.request;

import jakarta.validation.constraints.Future;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateUrlRequestDto {

    @Future(message = "Expiration time must be in the future")
    private LocalDateTime expiresAt;
}
