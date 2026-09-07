package com.thinh.shortener.controller;

import com.thinh.shortener.domain.dto.request.ChangePasswordRequestDto;
import com.thinh.shortener.domain.dto.request.UpdateProfileRequestDto;
import com.thinh.shortener.domain.dto.response.UserProfileResponseDto;
import com.thinh.shortener.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<UserProfileResponseDto> getProfile(Principal principal) {
        UserProfileResponseDto response = profileService.getProfile(principal.getName());
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<UserProfileResponseDto> updateProfile(
            @Valid @RequestBody UpdateProfileRequestDto request,
            Principal principal
    ) {
        UserProfileResponseDto response = profileService.updateProfile(request, principal.getName());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @Valid @RequestBody ChangePasswordRequestDto request,
            Principal principal
    ) {
        profileService.changePassword(request, principal.getName());
        return ResponseEntity.ok("Password changed successfully!");
    }
}
