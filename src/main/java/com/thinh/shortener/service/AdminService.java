package com.thinh.shortener.service;

import com.thinh.shortener.domain.dto.admin.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AdminService {

    AdminStatsResponseDto getStats();

    Page<AdminUserResponseDto> getUsers(String search, String role, Boolean isActive, Pageable pageable);

    AdminUserResponseDto getUserById(Long id);

    AdminUserResponseDto updateUser(Long id, AdminUpdateUserRequestDto request, String currentAdminEmail);

    void resetUserPassword(Long id, AdminResetPasswordRequestDto request, String currentAdminEmail);

    Page<AdminUrlResponseDto> getUserUrls(Long userId, String search, Boolean isActive, Pageable pageable);

    AdminUserResponseDto updateUserStatus(Long userId, UpdateUserStatusRequestDto request, String currentAdminEmail);

    AdminUserResponseDto assignUserRoles(Long userId, AssignUserRolesRequestDto request, String currentAdminEmail);

    Page<AdminUrlResponseDto> getUrls(String search, Boolean isActive, Pageable pageable);

    AdminUrlResponseDto updateUrlStatus(Long urlId, UpdateUrlStatusRequestDto request);

    void deleteUrl(Long urlId);

    List<RoleResponseDto> getRoles();

    List<PermissionResponseDto> getPermissions();

    RoleResponseDto createRole(CreateRoleRequestDto request);

    RoleResponseDto updateRole(Long roleId, UpdateRoleRequestDto request);

    void deleteRole(Long roleId);
}
