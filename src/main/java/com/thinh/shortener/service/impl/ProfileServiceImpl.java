package com.thinh.shortener.service.impl;

import com.thinh.shortener.domain.dto.request.ChangePasswordRequestDto;
import com.thinh.shortener.domain.dto.request.UpdateProfileRequestDto;
import com.thinh.shortener.domain.dto.response.UserProfileResponseDto;
import com.thinh.shortener.domain.entity.Permission;
import com.thinh.shortener.domain.entity.Role;
import com.thinh.shortener.domain.entity.User;
import com.thinh.shortener.domain.entity.UserProfile;
import com.thinh.shortener.domain.mapper.UserProfileMapper;
import com.thinh.shortener.exception.InvalidPasswordException;
import com.thinh.shortener.exception.ResourceNotFoundException;
import com.thinh.shortener.repository.UrlRepository;
import com.thinh.shortener.repository.UserProfileRepository;
import com.thinh.shortener.repository.UserRepository;
import com.thinh.shortener.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UrlRepository urlRepository;
    private final UserProfileMapper userProfileMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserProfileResponseDto getProfile(String email) {
        User user = userRepository.findByEmailWithRolesAndPermissions(email)
                .or(() -> userRepository.findByEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> userProfileRepository.save(UserProfile.builder().user(user).build()));

        UserProfileResponseDto response = userProfileMapper.toDto(profile);
        response.setTotalUrls(urlRepository.countByUserId(user.getId()));
        response.setTotalClicks(urlRepository.sumClickCountByUserId(user.getId()));

        if (user.getRoles() != null) {
            List<String> roleNames = user.getRoles().stream().map(Role::getName).toList();
            response.setRoles(roleNames);
            Set<String> perms = new HashSet<>(user.getRoles().stream()
                    .flatMap(r -> r.getPermissions().stream())
                    .map(Permission::getName)
                    .toList());
            if (roleNames.contains("SUPER_ADMIN")) {
                perms.addAll(List.of(
                        "USER_READ", "USER_MANAGE",
                        "URL_READ", "URL_DISABLE", "URL_DELETE",
                        "ANALYTICS_READ",
                        "ROLE_READ", "ROLE_MANAGE"
                ));
            }
            response.setPermissions(new ArrayList<>(perms));
        }

        log.debug("Retrieved profile for user: {}", email);
        return response;
    }

    @Override
    @Transactional
    public UserProfileResponseDto updateProfile(UpdateProfileRequestDto request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> UserProfile.builder().user(user).build());

        profile.setFullName(request.getFullName());
        profile.setAvatarUrl(request.getAvatarUrl());
        profile.setBio(request.getBio());
        profile.setPhoneNumber(request.getPhoneNumber());
        profile.setCompany(request.getCompany());

        profile = userProfileRepository.save(profile);
        log.info("User profile updated successfully: email={}", email);

        UserProfileResponseDto response = userProfileMapper.toDto(profile);
        response.setTotalUrls(urlRepository.countByUserId(user.getId()));
        response.setTotalClicks(urlRepository.sumClickCountByUserId(user.getId()));

        if (user.getRoles() != null) {
            response.setRoles(user.getRoles().stream().map(r -> r.getName()).toList());
            response.setPermissions(user.getRoles().stream()
                    .flatMap(r -> r.getPermissions().stream())
                    .map(p -> p.getName())
                    .distinct()
                    .toList());
        }

        return response;
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequestDto request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("Password change rejected: Incorrect current password for user: {}", email);
            throw new InvalidPasswordException("Current password is not correct!");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            log.warn("Password change rejected: Confirmation password mismatch for user: {}", email);
            throw new InvalidPasswordException("New password and confirmation password do not match!");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            log.warn("Password change rejected: New password cannot be same as old password for user: {}", email);
            throw new InvalidPasswordException("New password cannot be the same as current password!");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed successfully for user: {}", email);
    }
}
