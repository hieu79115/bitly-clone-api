package com.thinh.shortener.service;

import com.thinh.shortener.domain.dto.request.LoginRequestDto;
import com.thinh.shortener.domain.dto.request.RefreshTokenRequestDto;
import com.thinh.shortener.domain.dto.request.RegisterRequestDto;
import com.thinh.shortener.domain.dto.response.AuthResponseDto;
import com.thinh.shortener.domain.entity.Role;
import com.thinh.shortener.domain.entity.User;
import com.thinh.shortener.exception.EmailAlreadyExistsException;
import com.thinh.shortener.repository.UserRepository;
import com.thinh.shortener.security.CustomUserDetailsService;
import com.thinh.shortener.security.jwt.JwtTokenProvider;
import com.thinh.shortener.service.impl.AuthServiceImpl;
import com.thinh.shortener.util.RedisKeyConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests - AuthServiceImpl Business Logic")
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ==========================================
    // 1. REGISTER TESTS
    // ==========================================

    @Test
    @DisplayName("Should successfully register a new user when email is available")
    void register_Success() {
        // GIVEN
        RegisterRequestDto request = RegisterRequestDto.builder()
                .email("newuser@test.com")
                .password("password123")
                .build();

        when(userRepository.existsByEmail("newuser@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword123");

        // WHEN
        authService.register(request);

        // THEN
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw EmailAlreadyExistsException when email is already registered")
    void register_WhenEmailAlreadyExists_ShouldThrowException() {
        // GIVEN
        RegisterRequestDto request = RegisterRequestDto.builder()
                .email("existing@test.com")
                .password("password123")
                .build();

        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);

        // WHEN & THEN
        assertThrows(EmailAlreadyExistsException.class, () -> {
            authService.register(request);
        });
        verify(userRepository, never()).save(any(User.class));
    }

    // ==========================================
    // 2. LOGIN TESTS
    // ==========================================

    @Test
    @DisplayName("Should successfully authenticate and return token pair with Redis storage")
    void login_Success() {
        // GIVEN
        LoginRequestDto request = LoginRequestDto.builder()
                .email("user@test.com")
                .password("password123")
                .build();

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn("mockAccessToken");
        when(tokenProvider.generateRefreshToken("user@test.com")).thenReturn("mockRefreshToken");

        // WHEN
        AuthResponseDto response = authService.login(request);

        // THEN
        assertNotNull(response);
        assertEquals("mockAccessToken", response.getAccessToken());
        assertEquals("mockRefreshToken", response.getRefreshToken());
        assertEquals("user@test.com", response.getEmail());

        // Verify Refresh Token is saved to Redis with 7 days TTL
        verify(valueOperations, times(1)).set(
                eq(RedisKeyConstants.REFRESH_TOKEN_PREFIX + "user@test.com"),
                eq("mockRefreshToken"),
                eq(7L),
                eq(TimeUnit.DAYS)
        );
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when login credentials are invalid")
    void login_WhenCredentialsInvalid_ShouldThrowBadCredentialsException() {
        // GIVEN
        LoginRequestDto request = LoginRequestDto.builder()
                .email("user@test.com")
                .password("wrongpassword")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // WHEN & THEN
        assertThrows(BadCredentialsException.class, () -> {
            authService.login(request);
        });
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    // ==========================================
    // 3. REFRESH TOKEN TESTS
    // ==========================================

    @Test
    @DisplayName("Should successfully rotate refresh token and issue new token pair")
    void refreshToken_Success() {
        // GIVEN
        String oldRefreshToken = "validRefreshToken";
        RefreshTokenRequestDto request = RefreshTokenRequestDto.builder()
                .refreshToken(oldRefreshToken)
                .build();

        UserDetails mockUserDetails = org.springframework.security.core.userdetails.User.builder()
                .username("user@test.com")
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        when(tokenProvider.validateToken(oldRefreshToken)).thenReturn(true);
        when(tokenProvider.getTokenType(oldRefreshToken)).thenReturn(JwtTokenProvider.REFRESH_TOKEN_TYPE);
        when(tokenProvider.getEmailFromJWT(oldRefreshToken)).thenReturn("user@test.com");
        when(valueOperations.get(RedisKeyConstants.REFRESH_TOKEN_PREFIX + "user@test.com")).thenReturn(oldRefreshToken);
        when(customUserDetailsService.loadUserByUsername("user@test.com")).thenReturn(mockUserDetails);
        when(tokenProvider.generateToken(any())).thenReturn("newAccessToken");
        when(tokenProvider.generateRefreshToken("user@test.com")).thenReturn("newRefreshToken");

        // WHEN
        AuthResponseDto response = authService.refreshToken(request);

        // THEN
        assertNotNull(response);
        assertEquals("newAccessToken", response.getAccessToken());
        assertEquals("newRefreshToken", response.getRefreshToken());

        // Verify updated in Redis
        verify(valueOperations, times(1)).set(
                eq(RedisKeyConstants.REFRESH_TOKEN_PREFIX + "user@test.com"),
                eq("newRefreshToken"),
                eq(7L),
                eq(TimeUnit.DAYS)
        );
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when token type is not REFRESH")
    void refreshToken_WhenTokenTypeIsNotRefresh_ShouldThrowException() {
        // GIVEN
        String accessTokenUsedAsRefresh = "someAccessToken";
        RefreshTokenRequestDto request = RefreshTokenRequestDto.builder()
                .refreshToken(accessTokenUsedAsRefresh)
                .build();

        when(tokenProvider.validateToken(accessTokenUsedAsRefresh)).thenReturn(true);
        when(tokenProvider.getTokenType(accessTokenUsedAsRefresh)).thenReturn(JwtTokenProvider.ACCESS_TOKEN_TYPE);

        // WHEN & THEN
        assertThrows(BadCredentialsException.class, () -> {
            authService.refreshToken(request);
        });
    }

    // ==========================================
    // 4. LOGOUT TESTS
    // ==========================================

    @Test
    @DisplayName("Should put access token to Redis blacklist and delete refresh token")
    void logout_Success() {
        // GIVEN
        String accessToken = "validAccessToken";
        String email = "user@test.com";

        when(tokenProvider.getRemainingExpirationInMs(accessToken)).thenReturn(1800000L); // 30 minutes left

        // WHEN
        authService.logout(accessToken, email);

        // THEN
        // Verify Access Token is blacklisted with remaining TTL
        verify(valueOperations, times(1)).set(
                eq(RedisKeyConstants.BLACKLIST_PREFIX + accessToken),
                eq("blacklisted"),
                eq(1800000L),
                eq(TimeUnit.MILLISECONDS)
        );

        // Verify Refresh Token is deleted
        verify(redisTemplate, times(1)).delete(RedisKeyConstants.REFRESH_TOKEN_PREFIX + email);
    }
}
