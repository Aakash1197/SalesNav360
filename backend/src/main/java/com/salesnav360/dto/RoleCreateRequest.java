package com.salesnav360.dto;

import jakarta.validation.constraints.NotBlank;

public record RoleCreateRequest(@NotBlank String name) {
}

