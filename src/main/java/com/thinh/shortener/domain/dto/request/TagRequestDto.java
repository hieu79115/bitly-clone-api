package com.thinh.shortener.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TagRequestDto {

    @NotBlank(message = "Tag name must not be blank")
    @Size(min = 1, max = 50, message = "Tag name must be between 1 and 50 characters")
    private String name;
}
