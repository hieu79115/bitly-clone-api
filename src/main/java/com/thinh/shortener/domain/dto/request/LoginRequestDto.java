package com.thinh.shortener.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDto {
    @NotBlank(message = "Email must not be left blank.")
    private String email;

    @NotBlank(message = "The password cannot be left blank.")
    private String password;
}
