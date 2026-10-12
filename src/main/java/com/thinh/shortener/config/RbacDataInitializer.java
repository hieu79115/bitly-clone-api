package com.thinh.shortener.config;

import com.thinh.shortener.domain.entity.Permission;
import com.thinh.shortener.domain.entity.Role;
import com.thinh.shortener.domain.entity.User;
import com.thinh.shortener.domain.entity.UserProfile;
import com.thinh.shortener.repository.PermissionRepository;
import com.thinh.shortener.repository.RoleRepository;
import com.thinh.shortener.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class RbacDataInitializer implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Value("${app.root-admin.email:root@app.local}")
    private String rootAdminEmail;

    @org.springframework.beans.factory.annotation.Value("${app.root-admin.password:AdminPassword@123}")
    private String rootAdminPassword;

    @org.springframework.beans.factory.annotation.Value("${app.root-admin.reset-password:false}")
    private boolean resetRootAdminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking and initializing Dynamic RBAC permissions and roles...");

        // 0. Cleanup deprecated permissions
        cleanupDeprecatedPermissions();

        // 1. Initialize Permissions
        Map<String, Permission> permissions = initializePermissions();

        // 2. Initialize System Roles
        Map<String, Role> roles = initializeRoles(permissions);

        // 3. Initialize Root Super Admin Account
        initializeRootSuperAdmin(roles.get("SUPER_ADMIN"));

        // 4. Ensure any existing users without roles get assigned the default USER role
        migrateExistingUsersWithoutRoles(roles.get("USER"));

        log.info("Dynamic RBAC initialization completed successfully.");
    }

    private void cleanupDeprecatedPermissions() {
        permissionRepository.findByName("USER_DELETE").ifPresent(delPerm -> {
            List<Role> roles = roleRepository.findAllWithPermissions();
            for (Role role : roles) {
                if (role.getPermissions() != null && role.getPermissions().remove(delPerm)) {
                    roleRepository.save(role);
                }
            }
            permissionRepository.delete(delPerm);
            log.info("Cleaned up deprecated permission USER_DELETE from database.");
        });
    }

    private Map<String, Permission> initializePermissions() {
        Map<String, Permission> result = new HashMap<>();

        // Definition list: name, module, description
        List<String[]> permDefs = List.of(
                // USER Module
                new String[]{"USER_READ", "USER", "View user accounts and profiles"},
                new String[]{"USER_MANAGE", "USER", "Update user status, activate, or deactivate users"},

                // URL Module
                new String[]{"URL_READ", "URL", "View all short links across the platform"},
                new String[]{"URL_DISABLE", "URL", "Disable or enable links for safety/policy reasons"},
                new String[]{"URL_DELETE", "URL", "Permanently or soft delete links"},

                // ANALYTICS Module
                new String[]{"ANALYTICS_READ", "ANALYTICS", "View system-wide analytics, traffic volume, and KPI reports"},

                // RBAC Module
                new String[]{"ROLE_READ", "RBAC", "View roles and permission matrices"},
                new String[]{"ROLE_MANAGE", "RBAC", "Create custom roles, edit permissions, and assign roles to users"}
        );

        for (String[] def : permDefs) {
            String name = def[0];
            String module = def[1];
            String desc = def[2];

            Permission perm = permissionRepository.findByName(name)
                    .orElseGet(() -> {
                        Permission newPerm = Permission.builder()
                                .name(name)
                                .module(module)
                                .description(desc)
                                .build();
                        return permissionRepository.save(newPerm);
                    });
            result.put(name, perm);
        }

        return result;
    }

    private Map<String, Role> initializeRoles(Map<String, Permission> permMap) {
        Map<String, Role> result = new HashMap<>();

        // 1. SUPER_ADMIN: has ALL permissions
        Role superAdmin = roleRepository.findByNameWithPermissions("SUPER_ADMIN")
                .orElseGet(() -> Role.builder()
                        .name("SUPER_ADMIN")
                        .description("Root System Administrator with complete control over all modules, roles, and permissions.")
                        .isSystem(true)
                        .permissions(new HashSet<>())
                        .build());
        superAdmin.setPermissions(new HashSet<>(permMap.values()));
        superAdmin.setIsSystem(true);
        result.put("SUPER_ADMIN", roleRepository.save(superAdmin));

        // 2. ADMIN: User & URL & Analytics & Role read
        Set<Permission> adminPerms = new HashSet<>();
        addPermIfPresent(adminPerms, permMap, "USER_READ", "USER_MANAGE", "URL_READ", "URL_DISABLE", "URL_DELETE", "ANALYTICS_READ", "ROLE_READ");

        Role admin = roleRepository.findByNameWithPermissions("ADMIN")
                .orElseGet(() -> Role.builder()
                        .name("ADMIN")
                        .description("Administrator managing users, links, and system analytics.")
                        .isSystem(true)
                        .permissions(new HashSet<>())
                        .build());
        admin.setPermissions(adminPerms);
        admin.setIsSystem(true);
        result.put("ADMIN", roleRepository.save(admin));

        // 3. MODERATOR: Trust & Safety link reviewer
        Set<Permission> modPerms = new HashSet<>();
        addPermIfPresent(modPerms, permMap, "URL_READ", "URL_DISABLE", "ANALYTICS_READ", "USER_READ");

        Role moderator = roleRepository.findByNameWithPermissions("MODERATOR")
                .orElseGet(() -> Role.builder()
                        .name("MODERATOR")
                        .description("Content & Trust Safety Moderator to inspect and disable abusive or phishing links.")
                        .isSystem(false)
                        .permissions(new HashSet<>())
                        .build());
        moderator.setPermissions(modPerms);
        result.put("MODERATOR", roleRepository.save(moderator));

        // 4. USER: regular user
        Role userRole = roleRepository.findByNameWithPermissions("USER")
                .orElseGet(() -> Role.builder()
                        .name("USER")
                        .description("Standard platform user account.")
                        .isSystem(true)
                        .permissions(new HashSet<>())
                        .build());
        result.put("USER", roleRepository.save(userRole));

        return result;
    }

    private void addPermIfPresent(Set<Permission> target, Map<String, Permission> permMap, String... names) {
        for (String name : names) {
            Permission p = permMap.get(name);
            if (p != null) {
                target.add(p);
            }
        }
    }

    private void initializeRootSuperAdmin(Role superAdminRole) {
        Optional<User> existing = userRepository.findByEmail(rootAdminEmail);
        if (existing.isEmpty()) {
            User root = User.builder()
                    .email(rootAdminEmail)
                    .password(passwordEncoder.encode(rootAdminPassword))
                    .isActive(true)
                    .role("ROLE_ADMIN")
                    .roles(new HashSet<>(Collections.singletonList(superAdminRole)))
                    .build();

            UserProfile profile = UserProfile.builder()
                    .user(root)
                    .fullName("Root Administrator")
                    .bio("Master System Administrator (Root Owner)")
                    .build();
            root.setProfile(profile);

            userRepository.save(root);
            log.info("Default Root SuperAdmin account initialized: email={}", rootAdminEmail);
        } else {
            User root = existing.get();
            boolean needSave = false;

            if (root.getRoles() == null || !root.getRoles().contains(superAdminRole)) {
                if (root.getRoles() == null) root.setRoles(new HashSet<>());
                root.getRoles().add(superAdminRole);
                needSave = true;
                log.info("Ensured Root account has SUPER_ADMIN role.");
            }

            if (resetRootAdminPassword) {
                root.setPassword(passwordEncoder.encode(rootAdminPassword));
                needSave = true;
                log.warn("SECURITY NOTICE: Root SuperAdmin password has been forcibly reset from config/env: email={}", rootAdminEmail);
            }

            if (needSave) {
                userRepository.save(root);
            }
        }

        // Strictly enforce ONLY 1 Superadmin: demote any other user that might have been granted SUPER_ADMIN
        List<User> allUsers = userRepository.findAll();
        for (User u : allUsers) {
            if (!u.getEmail().equalsIgnoreCase(rootAdminEmail) && u.getRoles() != null) {
                boolean hadSuperAdmin = u.getRoles().removeIf(r -> "SUPER_ADMIN".equalsIgnoreCase(r.getName()));
                if (hadSuperAdmin) {
                    userRepository.save(u);
                    log.warn("Demoted unauthorized user [{}] from SUPER_ADMIN role (only [{}] is Root Owner).", u.getEmail(), rootAdminEmail);
                }
            }
        }
    }

    private void migrateExistingUsersWithoutRoles(Role userRole) {
        List<User> allUsers = userRepository.findAll();
        for (User u : allUsers) {
            if (u.getRoles() == null || u.getRoles().isEmpty()) {
                u.setRoles(new HashSet<>(Collections.singletonList(userRole)));
                userRepository.save(u);
            }
        }
    }
}
