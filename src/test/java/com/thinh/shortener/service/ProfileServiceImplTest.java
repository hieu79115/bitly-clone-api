package com.thinh.shortener.service;

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
import com.thinh.shortener.service.impl.ProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests - ProfileServiceImpl Business Logic")
class ProfileServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private UserProfileMapper userProfileMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ProfileServiceImpl profileService;

    private User mockUser;
    private UserProfile mockProfile;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .email("user@test.com")
                .password("encodedOldPassword")
                .build();

        mockProfile = UserProfile.builder()
                .id(10L)
                .fullName("John Doe")
                .user(mockUser)
                .build();
    }

    // ==========================================
    // 1. GET PROFILE TESTS
    // ==========================================

    @Test
    @DisplayName("Should successfully return user profile with total URLs and total clicks")
    void getProfile_Success() {
        // GIVEN
        UserProfileResponseDto expectedDto = UserProfileResponseDto.builder()
                .fullName("John Doe")
                .email("user@test.com")
                .build();

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(mockUser));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));
        when(userProfileMapper.toDto(mockProfile)).thenReturn(expectedDto);
        when(urlRepository.countByUserId(1L)).thenReturn(5L);
        when(urlRepository.sumClickCountByUserId(1L)).thenReturn(120L);

        // WHEN
        UserProfileResponseDto result = profileService.getProfile("user@test.com");

        // THEN
        assertNotNull(result);
        assertEquals(5L, result.getTotalUrls());
        assertEquals(120L, result.getTotalClicks());
    }

    // ==========================================
    // 2. UPDATE PROFILE TESTS
    // ==========================================

    @Test
    @DisplayName("Should update user profile information successfully")
    void updateProfile_Success() {
        // GIVEN
        UpdateProfileRequestDto request = UpdateProfileRequestDto.builder()
                .fullName("Jane Doe")
                .bio("Software Engineer")
                .phoneNumber("0987654321")
                .company("Tech Corp")
                .avatarUrl("https://avatar.url")
                .build();

        UserProfileResponseDto responseDto = UserProfileResponseDto.builder()
                .fullName("Jane Doe")
                .bio("Software Engineer")
                .build();

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(mockUser));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(mockProfile);
        when(userProfileMapper.toDto(any(UserProfile.class))).thenReturn(responseDto);

        // WHEN
        UserProfileResponseDto result = profileService.updateProfile(request, "user@test.com");

        // THEN
        assertNotNull(result);
        assertEquals("Jane Doe", result.getFullName());
        verify(userProfileRepository, times(1)).save(mockProfile);
    }

    // ==========================================
    // 3. CHANGE PASSWORD TESTS
    // ==========================================

    @Test
    @DisplayName("Should change password successfully when all validations pass")
    void changePassword_Success() {
        // GIVEN
        ChangePasswordRequestDto request = ChangePasswordRequestDto.builder()
                .currentPassword("oldPassword123")
                .newPassword("newPassword123@")
                .confirmPassword("newPassword123@")
                .build();

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("oldPassword123", "encodedOldPassword")).thenReturn(true);
        when(passwordEncoder.matches("newPassword123@", "encodedOldPassword")).thenReturn(false);
        when(passwordEncoder.encode("newPassword123@")).thenReturn("encodedNewPassword");

        // WHEN
        profileService.changePassword(request, "user@test.com");

        // THEN
        assertEquals("encodedNewPassword", mockUser.getPassword());
        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    @DisplayName("Should throw InvalidPasswordException when current password is wrong")
    void changePassword_WhenCurrentPasswordIsWrong_ShouldThrowException() {
        // GIVEN
        ChangePasswordRequestDto request = ChangePasswordRequestDto.builder()
                .currentPassword("wrongPassword")
                .newPassword("newPassword123@")
                .confirmPassword("newPassword123@")
                .build();

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("wrongPassword", "encodedOldPassword")).thenReturn(false);

        // WHEN & THEN
        InvalidPasswordException ex = assertThrows(InvalidPasswordException.class, () -> {
            profileService.changePassword(request, "user@test.com");
        });
        assertEquals("Current password is not correct!", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw InvalidPasswordException when confirm password does not match")
    void changePassword_WhenConfirmPasswordDoesNotMatch_ShouldThrowException() {
        // GIVEN
        ChangePasswordRequestDto request = ChangePasswordRequestDto.builder()
                .currentPassword("oldPassword123")
                .newPassword("newPassword123@")
                .confirmPassword("mismatchPassword")
                .build();

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("oldPassword123", "encodedOldPassword")).thenReturn(true);

        // WHEN & THEN
        InvalidPasswordException ex = assertThrows(InvalidPasswordException.class, () -> {
            profileService.changePassword(request, "user@test.com");
        });
        assertEquals("New password and confirmation password do not match!", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw InvalidPasswordException when new password is same as current password")
    void changePassword_WhenNewPasswordSameAsCurrent_ShouldThrowException() {
        // GIVEN
        ChangePasswordRequestDto request = ChangePasswordRequestDto.builder()
                .currentPassword("oldPassword123")
                .newPassword("oldPassword123")
                .confirmPassword("oldPassword123")
                .build();

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("oldPassword123", "encodedOldPassword")).thenReturn(true);

        // WHEN & THEN
        InvalidPasswordException ex = assertThrows(InvalidPasswordException.class, () -> {
            profileService.changePassword(request, "user@test.com");
        });
        assertEquals("New password cannot be the same as current password!", ex.getMessage());
    }
}
