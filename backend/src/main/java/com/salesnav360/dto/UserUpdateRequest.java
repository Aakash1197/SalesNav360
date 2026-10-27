package com.salesnav360.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record UserUpdateRequest(
        @NotNull Boolean enabled,
        @NotEmpty Set<@NotBlank String> roles) {
}

