package com.thinh.shortener.domain.dto.admin;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoleRequestDto {
    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    private List<String> permissionNames;
}
