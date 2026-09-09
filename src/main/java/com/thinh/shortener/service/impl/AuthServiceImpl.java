package com.thinh.shortener.service.impl;

import com.thinh.shortener.domain.dto.request.LoginRequestDto;
import com.thinh.shortener.domain.dto.request.RefreshTokenRequestDto;
import com.thinh.shortener.domain.dto.request.RegisterRequestDto;
import com.thinh.shortener.domain.dto.response.AuthResponseDto;
import com.thinh.shortener.domain.entity.Role;
import com.thinh.shortener.domain.entity.User;
import com.thinh.shortener.domain.entity.UserProfile;
import com.thinh.shortener.exception.EmailAlreadyExistsException;
import com.thinh.shortener.repository.UserRepository;
import com.thinh.shortener.security.CustomUserDetailsService;
import com.thinh.shortener.security.jwt.JwtTokenProvider;
import com.thinh.shortener.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.thinh.shortener.util.RedisKeyConstants;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final RedisTemplate<String, Object> redisTemplate;

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

        UserProfile profile = UserProfile.builder()
                .user(user)
                .build();
        user.setProfile(profile);

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

        String accessToken = tokenProvider.generateToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(request.getEmail());

        redisTemplate.opsForValue().set(
                RedisKeyConstants.REFRESH_TOKEN_PREFIX + request.getEmail(),
                refreshToken,
                7,
                TimeUnit.DAYS
        );

        return AuthResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .email(request.getEmail())
                .build();
    }

    @Override
    public AuthResponseDto refreshToken(RefreshTokenRequestDto request) {
        String refreshToken = request.getRefreshToken();

        if (!tokenProvider.validateToken(refreshToken)) {
            throw new BadCredentialsException("Invalid or expired refresh token!");
        }

        String tokenType = tokenProvider.getTokenType(refreshToken);
        if (!JwtTokenProvider.REFRESH_TOKEN_TYPE.equals(tokenType)) {
            throw new BadCredentialsException("Token is not a valid refresh token!");
        }

        String email = tokenProvider.getEmailFromJWT(refreshToken);

        String savedRefreshToken = (String) redisTemplate.opsForValue().get(RedisKeyConstants.REFRESH_TOKEN_PREFIX + email);
        if (savedRefreshToken == null || !savedRefreshToken.equals(refreshToken)) {
            throw new BadCredentialsException("Refresh token has been revoked or expired!");
        }

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        String newAccessToken = tokenProvider.generateToken(authentication);
        String newRefreshToken = tokenProvider.generateRefreshToken(email);

        redisTemplate.opsForValue().set(
                RedisKeyConstants.REFRESH_TOKEN_PREFIX + email,
                newRefreshToken,
                7,
                TimeUnit.DAYS
        );

        return AuthResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .email(email)
                .build();
    }

    @Override
    public void logout(String accessToken, String email) {
        if (StringUtils.hasText(accessToken)) {
            long remainingTimeMs = tokenProvider.getRemainingExpirationInMs(accessToken);
            if (remainingTimeMs > 0) {
                redisTemplate.opsForValue().set(
                        RedisKeyConstants.BLACKLIST_PREFIX + accessToken,
                        "blacklisted",
                        remainingTimeMs,
                        TimeUnit.MILLISECONDS
                );
            }

            if (!StringUtils.hasText(email)) {
                try {
                    email = tokenProvider.getEmailFromJWT(accessToken);
                } catch (Exception ignored) {
                }
            }
        }

        if (StringUtils.hasText(email)) {
            redisTemplate.delete(RedisKeyConstants.REFRESH_TOKEN_PREFIX + email);
        }

        SecurityContextHolder.clearContext();
    }

}
