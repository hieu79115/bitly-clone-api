package com.thinh.shortener.domain.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequestDto {
    @NotBlank(message = "Email must not be left blank.")
    @Email(message = "Invalid email format.")
    private String email;

    @NotBlank(message = "The password cannot be left blank.")
    @Size(min = 6, message = "The password must be at least 6 characters.")
    private String password;
}
