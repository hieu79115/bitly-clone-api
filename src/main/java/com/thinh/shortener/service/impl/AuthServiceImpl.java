package com.thinh.shortener.service.impl;

import com.thinh.shortener.domain.dto.request.LoginRequestDto;
import com.thinh.shortener.domain.dto.request.RegisterRequestDto;
import com.thinh.shortener.domain.dto.response.AuthResponseDto;
import com.thinh.shortener.domain.entity.Role;
import com.thinh.shortener.domain.entity.User;
import com.thinh.shortener.exception.EmailAlreadyExistsException;
import com.thinh.shortener.repository.UserRepository;
import com.thinh.shortener.security.jwt.JwtTokenProvider;
import com.thinh.shortener.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Override
    public void register(RegisterRequestDto request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email has already been registered!");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ROLE_USER)
                .build();

        userRepository.save(user);
    }

    @Override
    public AuthResponseDto login(LoginRequestDto request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = tokenProvider.generateToken(authentication);

        return AuthResponseDto.builder()
                .accessToken(jwt)
                .email(request.getEmail())
                .build();
    }


}
