package com.salesnav360.dto;

import com.salesnav360.enums.ClientStatus;
import com.salesnav360.enums.ClientType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClientRequest(
        @NotBlank @Size(max = 120) String fullName,
        @Email @Size(max = 255) String email,
        @Size(max = 30) String phone,
        @Size(max = 160) String companyName,
        @NotNull ClientType type,
        @NotNull ClientStatus status,
        @Size(max = 80) String leadSource,
        @Size(max = 80) String assignedTo,
        @Size(max = 2000) String notes) {
}

