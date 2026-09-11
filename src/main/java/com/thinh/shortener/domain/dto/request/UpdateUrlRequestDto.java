package com.thinh.shortener.domain.dto.request;

import jakarta.validation.constraints.Future;
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

    @Future(message = "Expiration time must be in the future")
    private LocalDateTime expiresAt;

    private Set<Long> tagIds;
}
