package com.thinh.shortener.domain.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleResponseDto {
    private Long id;
    private String name;
    private String description;
    private Boolean isSystem;
    private List<PermissionResponseDto> permissions;
    private long userCount;
    private LocalDateTime createdAt;
}
