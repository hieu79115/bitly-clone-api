package com.thinh.shortener.domain.dto.admin;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignUserRolesRequestDto {
    @NotEmpty(message = "roleNames cannot be empty")
    private List<String> roleNames;
}
