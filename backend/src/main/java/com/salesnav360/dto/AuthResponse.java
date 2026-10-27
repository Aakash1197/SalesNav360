package com.salesnav360.dto;

import java.util.Set;

public record AuthResponse(
        Long userId,
        String username,
        String email,
        Set<String> roles) {
}

