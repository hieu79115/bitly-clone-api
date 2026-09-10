package com.thinh.shortener.domain.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
@Builder
public class UpdateProfileRequestDto {

    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;

    @URL(message = "Invalid avatar URL format")
    private String avatarUrl;

    @Size(max = 255, message = "Bio must not exceed 255 characters")
    private String bio;

    @Pattern(regexp = "^$|^[0-9+]{9,15}$", message = "Invalid phone number format")
    private String phoneNumber;

    @Size(max = 100, message = "Company name must not exceed 100 characters")
    private String company;
}
