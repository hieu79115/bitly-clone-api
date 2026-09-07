package com.thinh.shortener.service;

import com.thinh.shortener.domain.dto.request.ChangePasswordRequestDto;
import com.thinh.shortener.domain.dto.request.UpdateProfileRequestDto;
import com.thinh.shortener.domain.dto.response.UserProfileResponseDto;

public interface ProfileService {
    UserProfileResponseDto getProfile(String email);

    UserProfileResponseDto updateProfile(UpdateProfileRequestDto request, String email);

    void changePassword(ChangePasswordRequestDto request, String email);
}
