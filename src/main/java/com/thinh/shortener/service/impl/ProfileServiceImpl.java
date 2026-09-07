package com.thinh.shortener.service.impl;

import com.thinh.shortener.domain.dto.request.ChangePasswordRequestDto;
import com.thinh.shortener.domain.dto.request.UpdateProfileRequestDto;
import com.thinh.shortener.domain.dto.response.UserProfileResponseDto;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> userProfileRepository.save(UserProfile.builder().user(user).build()));

        UserProfileResponseDto response = userProfileMapper.toDto(profile);
        response.setTotalUrls(urlRepository.countByUserId(user.getId()));
        response.setTotalClicks(urlRepository.sumClickCountByUserId(user.getId()));

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

        UserProfileResponseDto response = userProfileMapper.toDto(profile);
        response.setTotalUrls(urlRepository.countByUserId(user.getId()));
        response.setTotalClicks(urlRepository.sumClickCountByUserId(user.getId()));

        return response;
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequestDto request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Current password is not correct!");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new InvalidPasswordException("New password and confirmation password do not match!");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new InvalidPasswordException("New password cannot be the same as current password!");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
