package com.thinh.shortener.domain.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUrlStatusRequestDto {
    @NotNull(message = "isActive must not be null")
    private Boolean isActive;
}
