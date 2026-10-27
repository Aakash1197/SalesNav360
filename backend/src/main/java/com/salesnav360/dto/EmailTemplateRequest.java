package com.salesnav360.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailTemplateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 255) String subject,
        @NotBlank @Size(max = 10000) String body) {
}
