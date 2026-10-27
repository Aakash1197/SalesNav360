package com.salesnav360.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record UserCreateRequest(
        @NotBlank
        @Size(max = 80)
        @Pattern(regexp = "^[A-Za-z0-9._-]+$")
        String username,
        @NotBlank
        @Email
        @Size(max = 255)
        String email,
        @NotBlank
        @Size(min = 12, max = 72)
        String password,
        @NotEmpty
        Set<@NotBlank String> roles) {
}

