package com.thinh.shortener.service;

import com.thinh.shortener.domain.dto.request.LoginRequestDto;
import com.thinh.shortener.domain.dto.request.RegisterRequestDto;
import com.thinh.shortener.domain.dto.response.AuthResponseDto;

public interface AuthService {
    void register(RegisterRequestDto request);

    AuthResponseDto login(LoginRequestDto request);
}
