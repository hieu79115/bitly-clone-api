package com.thinh.shortener.controller;

import com.thinh.shortener.domain.dto.admin.*;
import com.thinh.shortener.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // --- 1. SYSTEM OVERVIEW STATS ---
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('ANALYTICS_READ')")
    public ResponseEntity<AdminStatsResponseDto> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    // --- 2. USER MANAGEMENT ---
    @GetMapping("/users")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<Page<AdminUserResponseDto>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(adminService.getUsers(search, role, isActive, pageable));
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<AdminUserResponseDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUserById(id));
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<AdminUserResponseDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateUserRequestDto request,
            Principal principal
    ) {
        return ResponseEntity.ok(adminService.updateUser(id, request, principal.getName()));
    }

    @PostMapping("/users/{id}/reset-password")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<Void> resetUserPassword(
            @PathVariable Long id,
            @Valid @RequestBody AdminResetPasswordRequestDto request,
            Principal principal
    ) {
        adminService.resetUserPassword(id, request, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{id}/urls")
    @PreAuthorize("hasAuthority('URL_READ')")
    public ResponseEntity<Page<AdminUrlResponseDto>> getUserUrls(
            @PathVariable Long id,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(adminService.getUserUrls(id, search, isActive, pageable));
    }

    @PatchMapping("/users/{id}/status")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<AdminUserResponseDto> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequestDto request,
            Principal principal
    ) {
        return ResponseEntity.ok(adminService.updateUserStatus(id, request, principal.getName()));
    }

    @PatchMapping("/users/{id}/roles")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<AdminUserResponseDto> assignUserRoles(
            @PathVariable Long id,
            @Valid @RequestBody AssignUserRolesRequestDto request,
            Principal principal
    ) {
        return ResponseEntity.ok(adminService.assignUserRoles(id, request, principal.getName()));
    }

    // --- 3. URL MANAGEMENT ---
    @GetMapping("/urls")
    @PreAuthorize("hasAuthority('URL_READ')")
    public ResponseEntity<Page<AdminUrlResponseDto>> getUrls(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(adminService.getUrls(search, isActive, pageable));
    }

    @PatchMapping("/urls/{id}/status")
    @PreAuthorize("hasAuthority('URL_DISABLE')")
    public ResponseEntity<AdminUrlResponseDto> updateUrlStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUrlStatusRequestDto request
    ) {
        return ResponseEntity.ok(adminService.updateUrlStatus(id, request));
    }

    @DeleteMapping("/urls/{id}")
    @PreAuthorize("hasAuthority('URL_DELETE')")
    public ResponseEntity<Void> deleteUrl(@PathVariable Long id) {
        adminService.deleteUrl(id);
        return ResponseEntity.noContent().build();
    }

    // --- 4. DYNAMIC ROLES & PERMISSIONS ---
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public ResponseEntity<List<RoleResponseDto>> getRoles() {
        return ResponseEntity.ok(adminService.getRoles());
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public ResponseEntity<List<PermissionResponseDto>> getPermissions() {
        return ResponseEntity.ok(adminService.getPermissions());
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<RoleResponseDto> createRole(@Valid @RequestBody CreateRoleRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createRole(request));
    }

    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<RoleResponseDto> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequestDto request
    ) {
        return ResponseEntity.ok(adminService.updateRole(id, request));
    }

    @DeleteMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        adminService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }
}
