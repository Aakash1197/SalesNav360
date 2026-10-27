package com.salesnav360.dto;

import com.salesnav360.enums.PolicyStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PolicyRequest(
        @NotBlank @Size(max = 80) String policyNumber,
        @NotBlank @Size(max = 80) String policyType,
        @NotNull Long clientId,
        @NotBlank @Size(max = 160) String provider,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal premiumAmount,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal coverageAmount,
        @NotNull LocalDate startDate,
        @NotNull LocalDate expiryDate,
        LocalDate renewalDate,
        @NotNull PolicyStatus status,
        @Size(max = 80) String assignedTo) {
}

