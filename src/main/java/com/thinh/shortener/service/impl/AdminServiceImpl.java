package com.thinh.shortener.service.impl;

import com.thinh.shortener.domain.dto.admin.*;
import com.thinh.shortener.domain.entity.Permission;
import com.thinh.shortener.domain.entity.Role;
import com.thinh.shortener.domain.entity.Url;
import com.thinh.shortener.domain.entity.User;
import com.thinh.shortener.domain.entity.UserProfile;
import com.thinh.shortener.exception.ResourceNotFoundException;
import com.thinh.shortener.repository.PermissionRepository;
import com.thinh.shortener.repository.RoleRepository;
import com.thinh.shortener.repository.UrlRepository;
import com.thinh.shortener.repository.UserRepository;
import com.thinh.shortener.service.AdminService;
import com.thinh.shortener.util.RedisKeyConstants;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final UrlRepository urlRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.domain}")
    private String appDomain;

    @Override
    @Transactional(readOnly = true)
    public AdminStatsResponseDto getStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByIsActiveTrue();
        long bannedUsers = userRepository.countByIsActiveFalse();

        long totalUrls = urlRepository.count();
        long activeUrls = urlRepository.countByIsActiveTrue();
        long disabledUrls = urlRepository.countByIsActiveFalse();

        long totalClicks = urlRepository.sumAllClickCount();
        long totalRoles = roleRepository.count();

        Pageable top5 = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<AdminUserResponseDto> recentUsers = userRepository.findAll(top5).stream()
                .map(this::mapToUserResponseDto)
                .toList();

        List<AdminUrlResponseDto> recentUrls = urlRepository.findAll(top5).stream()
                .map(this::mapToUrlResponseDto)
                .toList();

        return AdminStatsResponseDto.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .bannedUsers(bannedUsers)
                .totalUrls(totalUrls)
                .activeUrls(activeUrls)
                .disabledUrls(disabledUrls)
                .totalClicks(totalClicks)
                .totalRoles(totalRoles)
                .recentUsers(recentUsers)
                .recentUrls(recentUrls)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserResponseDto> getUsers(String search, String role, Boolean isActive, Pageable pageable) {
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(root.join("profile", JoinType.LEFT).get("fullName")), pattern)
                ));
            }

            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }

            if (StringUtils.hasText(role)) {
                predicates.add(cb.equal(root.join("roles", JoinType.LEFT).get("name"), role.trim().toUpperCase()));
            }

            if (query != null) {
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<User> userPage = userRepository.findAll(spec, pageable);
        return userPage.map(this::mapToUserResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponseDto getUserById(Long id) {
        User user = userRepository.findByIdWithRolesAndPermissions(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapToUserResponseDto(user);
    }

    @Override
    @Transactional
    public AdminUserResponseDto updateUser(Long id, AdminUpdateUserRequestDto request, String currentAdminEmail) {
        User user = userRepository.findByIdWithRolesAndPermissions(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        UserProfile profile = user.getProfile();
        if (profile == null) {
            profile = UserProfile.builder().user(user).build();
            user.setProfile(profile);
        }

        if (request.getFullName() != null) profile.setFullName(request.getFullName().trim());
        if (request.getBio() != null) profile.setBio(request.getBio().trim());
        if (request.getCompany() != null) profile.setCompany(request.getCompany().trim());
        if (request.getPhoneNumber() != null) profile.setPhoneNumber(request.getPhoneNumber().trim());
        if (request.getAvatarUrl() != null) {
            String av = request.getAvatarUrl().trim();
            profile.setAvatarUrl(av.isEmpty() ? null : av);
        }

        if (request.getIsActive() != null) {
            if (user.getEmail().equalsIgnoreCase(currentAdminEmail) && !request.getIsActive()) {
                throw new IllegalArgumentException("Action rejected: You cannot deactivate your own account.");
            }
            boolean isSuperAdmin = user.getRoles().stream().anyMatch(r -> "SUPER_ADMIN".equalsIgnoreCase(r.getName()));
            if (isSuperAdmin && !request.getIsActive()) {
                throw new IllegalArgumentException("Action rejected: Super Admin accounts cannot be deactivated.");
            }
            user.setIsActive(request.getIsActive());

            if (!request.getIsActive()) {
                redisTemplate.delete(RedisKeyConstants.REFRESH_TOKEN_PREFIX + user.getEmail());
            }
        }

        User saved = userRepository.save(user);
        log.info("Admin [{}] updated profile for user [{}]", currentAdminEmail, user.getEmail());
        return mapToUserResponseDto(saved);
    }

    @Override
    @Transactional
    public void resetUserPassword(Long id, AdminResetPasswordRequestDto request, String currentAdminEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setPassword(passwordEncoder.encode(request.getNewPassword().trim()));
        userRepository.save(user);

        // Revoke active refresh token from Redis to force re-login
        redisTemplate.delete(RedisKeyConstants.REFRESH_TOKEN_PREFIX + user.getEmail());
        log.info("Admin [{}] reset password for user [{}]", currentAdminEmail, user.getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUrlResponseDto> getUserUrls(Long userId, String search, Boolean isActive, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        Specification<Url> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user").get("id"), userId));

            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("shortCode")), pattern),
                        cb.like(cb.lower(root.get("originalUrl")), pattern),
                        cb.like(cb.lower(root.get("title")), pattern)
                ));
            }

            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return urlRepository.findAll(spec, pageable).map(this::mapToUrlResponseDto);
    }

    private AdminUserResponseDto mapToUserResponseDto(User u) {
        List<String> roles = u.getRoles() != null
                ? u.getRoles().stream().map(Role::getName).toList()
                : Collections.emptyList();

        String fullName = u.getProfile() != null ? u.getProfile().getFullName() : null;
        String avatarUrl = u.getProfile() != null ? u.getProfile().getAvatarUrl() : null;
        String bio = u.getProfile() != null ? u.getProfile().getBio() : null;
        String company = u.getProfile() != null ? u.getProfile().getCompany() : null;
        String phoneNumber = u.getProfile() != null ? u.getProfile().getPhoneNumber() : null;

        long totalUrls = urlRepository.countByUserId(u.getId());
        long totalClicks = urlRepository.sumClickCountByUserId(u.getId());

        return AdminUserResponseDto.builder()
                .id(u.getId())
                .email(u.getEmail())
                .fullName(fullName)
                .avatarUrl(avatarUrl)
                .bio(bio)
                .company(company)
                .phoneNumber(phoneNumber)
                .isActive(u.getIsActive() != null ? u.getIsActive() : true)
                .roles(roles)
                .totalUrls(totalUrls)
                .totalClicks(totalClicks)
                .createdAt(u.getCreatedAt())
                .build();
    }

    private AdminUrlResponseDto mapToUrlResponseDto(Url u) {
        String domain = appDomain.endsWith("/") ? appDomain : appDomain + "/";
        return AdminUrlResponseDto.builder()
                .id(u.getId())
                .title(u.getTitle())
                .originalUrl(u.getOriginalUrl())
                .shortCode(u.getShortCode())
                .shortUrl(domain + u.getShortCode())
                .userEmail(u.getUser() != null ? u.getUser().getEmail() : null)
                .userId(u.getUser() != null ? u.getUser().getId() : null)
                .clickCount(u.getClickCount())
                .isActive(u.getIsActive() != null ? u.getIsActive() : true)
                .createdAt(u.getCreatedAt())
                .expiresAt(u.getExpiresAt())
                .build();
    }

    @Override
    @Transactional
    public AdminUserResponseDto updateUserStatus(Long userId, UpdateUserStatusRequestDto request, String currentAdminEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Self-lockout prevention
        if (user.getEmail().equalsIgnoreCase(currentAdminEmail)) {
            throw new IllegalArgumentException("Action rejected: You cannot deactivate your own account.");
        }

        // Super Admin protection
        boolean isSuperAdmin = user.getRoles().stream().anyMatch(r -> "SUPER_ADMIN".equalsIgnoreCase(r.getName()));
        if (isSuperAdmin && !request.getIsActive()) {
            throw new IllegalArgumentException("Action rejected: Super Admin accounts cannot be deactivated.");
        }

        user.setIsActive(request.getIsActive());
        User saved = userRepository.save(user);

        log.info("Admin {} updated status of user {} to isActive={}", currentAdminEmail, user.getEmail(), request.getIsActive());

        // If user is banned, revoke their active refresh token from Redis
        if (!request.getIsActive()) {
            redisTemplate.delete(RedisKeyConstants.REFRESH_TOKEN_PREFIX + user.getEmail());
        }

        List<String> roles = saved.getRoles().stream().map(Role::getName).toList();
        String fullName = saved.getProfile() != null ? saved.getProfile().getFullName() : null;

        return AdminUserResponseDto.builder()
                .id(saved.getId())
                .email(saved.getEmail())
                .fullName(fullName)
                .isActive(saved.getIsActive())
                .roles(roles)
                .totalUrls(urlRepository.countByUserId(saved.getId()))
                .totalClicks(urlRepository.sumClickCountByUserId(saved.getId()))
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public AdminUserResponseDto assignUserRoles(Long userId, AssignUserRolesRequestDto request, String currentAdminEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Self-demotion prevention
        boolean isCurrentlySuperAdmin = user.getRoles().stream().anyMatch(r -> "SUPER_ADMIN".equalsIgnoreCase(r.getName()));
        if (user.getEmail().equalsIgnoreCase(currentAdminEmail) && isCurrentlySuperAdmin) {
            boolean willKeepSuperAdmin = request.getRoleNames().stream().anyMatch("SUPER_ADMIN"::equalsIgnoreCase);
            if (!willKeepSuperAdmin) {
                throw new IllegalArgumentException("Action rejected: You cannot remove SUPER_ADMIN role from yourself.");
            }
        }

        List<Role> matchedRoles = roleRepository.findByNameIn(request.getRoleNames());
        if (matchedRoles.isEmpty()) {
            throw new IllegalArgumentException("No valid roles found for names: " + request.getRoleNames());
        }

        user.setRoles(new HashSet<>(matchedRoles));
        User saved = userRepository.save(user);

        log.info("Admin {} updated roles of user {} to {}", currentAdminEmail, user.getEmail(), request.getRoleNames());

        List<String> roles = saved.getRoles().stream().map(Role::getName).toList();
        String fullName = saved.getProfile() != null ? saved.getProfile().getFullName() : null;

        return AdminUserResponseDto.builder()
                .id(saved.getId())
                .email(saved.getEmail())
                .fullName(fullName)
                .isActive(saved.getIsActive())
                .roles(roles)
                .totalUrls(urlRepository.countByUserId(saved.getId()))
                .totalClicks(urlRepository.sumClickCountByUserId(saved.getId()))
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUrlResponseDto> getUrls(String search, Boolean isActive, Pageable pageable) {
        Specification<Url> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("shortCode")), pattern),
                        cb.like(cb.lower(root.get("originalUrl")), pattern),
                        cb.like(cb.lower(root.get("title")), pattern)
                ));
            }

            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Url> urlPage = urlRepository.findAll(spec, pageable);

        String domain = appDomain.endsWith("/") ? appDomain : appDomain + "/";

        return urlPage.map(u -> AdminUrlResponseDto.builder()
                .id(u.getId())
                .title(u.getTitle())
                .originalUrl(u.getOriginalUrl())
                .shortCode(u.getShortCode())
                .shortUrl(domain + u.getShortCode())
                .userEmail(u.getUser() != null ? u.getUser().getEmail() : null)
                .userId(u.getUser() != null ? u.getUser().getId() : null)
                .clickCount(u.getClickCount())
                .isActive(u.getIsActive() != null ? u.getIsActive() : true)
                .createdAt(u.getCreatedAt())
                .expiresAt(u.getExpiresAt())
                .build());
    }

    @Override
    @Transactional
    public AdminUrlResponseDto updateUrlStatus(Long urlId, UpdateUrlStatusRequestDto request) {
        Url url = urlRepository.findById(urlId)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found with id: " + urlId));

        url.setIsActive(request.getIsActive());
        Url saved = urlRepository.save(url);

        // Immediate Redis cache eviction to prevent stale redirection
        String cacheKey = RedisKeyConstants.URL_PREFIX + url.getShortCode();
        redisTemplate.delete(cacheKey);

        log.info("URL id={} shortCode={} updated status to isActive={}. Cache evicted.", urlId, url.getShortCode(), request.getIsActive());

        String domain = appDomain.endsWith("/") ? appDomain : appDomain + "/";

        return AdminUrlResponseDto.builder()
                .id(saved.getId())
                .title(saved.getTitle())
                .originalUrl(saved.getOriginalUrl())
                .shortCode(saved.getShortCode())
                .shortUrl(domain + saved.getShortCode())
                .userEmail(saved.getUser() != null ? saved.getUser().getEmail() : null)
                .userId(saved.getUser() != null ? saved.getUser().getId() : null)
                .clickCount(saved.getClickCount())
                .isActive(saved.getIsActive())
                .createdAt(saved.getCreatedAt())
                .expiresAt(saved.getExpiresAt())
                .build();
    }

    @Override
    @Transactional
    public void deleteUrl(Long urlId) {
        Url url = urlRepository.findById(urlId)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found with id: " + urlId));

        String cacheKey = RedisKeyConstants.URL_PREFIX + url.getShortCode();
        redisTemplate.delete(cacheKey);

        urlRepository.delete(url);
        log.info("URL id={} shortCode={} deleted by admin. Cache evicted.", urlId, url.getShortCode());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponseDto> getRoles() {
        List<Role> roles = roleRepository.findAllWithPermissions();

        return roles.stream().map(r -> {
            List<PermissionResponseDto> permissions = r.getPermissions().stream()
                    .map(p -> PermissionResponseDto.builder()
                            .id(p.getId())
                            .name(p.getName())
                            .description(p.getDescription())
                            .module(p.getModule())
                            .build())
                    .sorted(Comparator.comparing(PermissionResponseDto::getModule).thenComparing(PermissionResponseDto::getName))
                    .toList();

            long userCount = userRepository.countUsersByRoleId(r.getId());

            return RoleResponseDto.builder()
                    .id(r.getId())
                    .name(r.getName())
                    .description(r.getDescription())
                    .isSystem(r.getIsSystem() != null ? r.getIsSystem() : false)
                    .permissions(permissions)
                    .userCount(userCount)
                    .createdAt(r.getCreatedAt())
                    .build();
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponseDto> getPermissions() {
        return permissionRepository.findAll().stream()
                .map(p -> PermissionResponseDto.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .description(p.getDescription())
                        .module(p.getModule())
                        .build())
                .sorted(Comparator.comparing(PermissionResponseDto::getModule).thenComparing(PermissionResponseDto::getName))
                .toList();
    }

    @Override
    @Transactional
    public RoleResponseDto createRole(CreateRoleRequestDto request) {
        String roleName = request.getName().trim().toUpperCase();

        if (roleRepository.existsByName(roleName)) {
            throw new IllegalArgumentException("Role with name " + roleName + " already exists.");
        }

        Set<Permission> permissions = new HashSet<>();
        if (request.getPermissionNames() != null && !request.getPermissionNames().isEmpty()) {
            permissions.addAll(permissionRepository.findByNameIn(request.getPermissionNames()));
        }

        Role role = Role.builder()
                .name(roleName)
                .description(request.getDescription())
                .isSystem(false)
                .permissions(permissions)
                .build();

        Role saved = roleRepository.save(role);
        log.info("Created new custom role: {}", roleName);

        List<PermissionResponseDto> permDtos = saved.getPermissions().stream()
                .map(p -> PermissionResponseDto.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .description(p.getDescription())
                        .module(p.getModule())
                        .build())
                .toList();

        return RoleResponseDto.builder()
                .id(saved.getId())
                .name(saved.getName())
                .description(saved.getDescription())
                .isSystem(saved.getIsSystem())
                .permissions(permDtos)
                .userCount(0)
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public RoleResponseDto updateRole(Long roleId, UpdateRoleRequestDto request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        if (StringUtils.hasText(request.getDescription())) {
            role.setDescription(request.getDescription());
        }

        if (request.getPermissionNames() != null) {
            // Cannot remove critical permissions from system roles
            if (Boolean.TRUE.equals(role.getIsSystem()) && "SUPER_ADMIN".equalsIgnoreCase(role.getName())) {
                throw new IllegalArgumentException("Cannot modify permissions of the root SUPER_ADMIN role.");
            }

            List<Permission> matched = permissionRepository.findByNameIn(request.getPermissionNames());
            role.setPermissions(new HashSet<>(matched));
        }

        Role saved = roleRepository.save(role);
        log.info("Updated role id={} name={}", roleId, role.getName());

        List<PermissionResponseDto> permDtos = saved.getPermissions().stream()
                .map(p -> PermissionResponseDto.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .description(p.getDescription())
                        .module(p.getModule())
                        .build())
                .toList();

        return RoleResponseDto.builder()
                .id(saved.getId())
                .name(saved.getName())
                .description(saved.getDescription())
                .isSystem(saved.getIsSystem())
                .permissions(permDtos)
                .userCount(userRepository.countUsersByRoleId(saved.getId()))
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void deleteRole(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        if (Boolean.TRUE.equals(role.getIsSystem())) {
            throw new IllegalArgumentException("Cannot delete system role: " + role.getName());
        }

        long assignedUsers = userRepository.countUsersByRoleId(roleId);
        if (assignedUsers > 0) {
            throw new IllegalArgumentException("Cannot delete role " + role.getName() + " because it is currently assigned to " + assignedUsers + " users. Reassign them first.");
        }

        roleRepository.delete(role);
        log.info("Deleted custom role id={} name={}", roleId, role.getName());
    }
}
