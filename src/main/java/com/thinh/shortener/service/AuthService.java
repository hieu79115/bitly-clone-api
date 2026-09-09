package com.thinh.shortener.service;

import com.thinh.shortener.domain.dto.request.LoginRequestDto;
import com.thinh.shortener.domain.dto.request.RefreshTokenRequestDto;
import com.thinh.shortener.domain.dto.request.RegisterRequestDto;
import com.thinh.shortener.domain.dto.response.AuthResponseDto;

public interface AuthService {
    void register(RegisterRequestDto request);

    AuthResponseDto login(LoginRequestDto request);

    AuthResponseDto refreshToken(RefreshTokenRequestDto request);

    void logout(String accessToken, String email);
}
