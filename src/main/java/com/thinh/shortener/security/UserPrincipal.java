package com.thinh.shortener.security;

import com.thinh.shortener.domain.entity.Permission;
import com.thinh.shortener.domain.entity.Role;
import com.thinh.shortener.domain.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Getter
@AllArgsConstructor
public class UserPrincipal implements UserDetails {
    private Long id;
    private String email;
    private String password;
    private boolean active;
    private Set<String> roleNames;
    private Set<String> permissionNames;
    private Collection<? extends GrantedAuthority> authorities;

    public static UserPrincipal create(User user) {
        Set<String> roleNames = new HashSet<>();
        Set<String> permissionNames = new HashSet<>();
        Set<GrantedAuthority> authorities = new HashSet<>();

        if (user.getRoles() != null) {
            for (Role role : user.getRoles()) {
                roleNames.add(role.getName());
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
                if (role.getPermissions() != null) {
                    for (Permission permission : role.getPermissions()) {
                        permissionNames.add(permission.getName());
                        authorities.add(new SimpleGrantedAuthority(permission.getName()));
                    }
                }
            }
        }

        // If the user has SUPER_ADMIN role, guarantee they have all permissions
        if (roleNames.contains("SUPER_ADMIN")) {
            for (String p : java.util.List.of(
                    "USER_READ", "USER_MANAGE",
                    "URL_READ", "URL_DISABLE", "URL_DELETE",
                    "ANALYTICS_READ",
                    "ROLE_READ", "ROLE_MANAGE"
            )) {
                permissionNames.add(p);
                authorities.add(new SimpleGrantedAuthority(p));
            }
        }

        boolean isActive = user.getIsActive() == null || user.getIsActive();

        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                isActive,
                roleNames,
                permissionNames,
                authorities
        );
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
