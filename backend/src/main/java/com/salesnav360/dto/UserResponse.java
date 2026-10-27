package com.salesnav360.dto;

import com.salesnav360.entity.UserAccount;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

public record UserResponse(
        Long id,
        String username,
        String email,
        boolean enabled,
        Set<String> roles,
        Instant createdAt,
        Instant updatedAt) {

    public static UserResponse from(UserAccount account) {
        return new UserResponse(
                account.getId(),
                account.getUsername(),
                account.getEmail(),
                account.isEnabled(),
                account.getRoles().stream().map(role -> role.getName()).collect(Collectors.toUnmodifiableSet()),
                account.getCreatedAt(),
                account.getUpdatedAt());
    }
}

