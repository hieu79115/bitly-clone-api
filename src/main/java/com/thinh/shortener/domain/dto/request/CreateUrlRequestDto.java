package com.thinh.shortener.domain.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class CreateUrlRequestDto {
    @NotBlank(message = "The original URL cannot be left blank.")
    @URL(message = "Invalid URL format")
    private String originalUrl;

    @Pattern(regexp = "^$|^[a-zA-Z0-9_-]{4,20}$", message = "Alias must be between 4 and 20 characters or left empty")
    private String customAlias;

    @Future(message = "Expiration time must be in the future")
    private LocalDateTime expiresAt;

    private Set<Long> tagIds;
}
