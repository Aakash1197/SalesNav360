package com.salesnav360.dto;

import com.salesnav360.enums.LeadPriority;
import com.salesnav360.enums.LeadStage;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record LeadRequest(
        @NotBlank @Size(max = 120) String fullName,
        @Email @Size(max = 255) String email,
        @Size(max = 30) String phone,
        @Size(max = 160) String companyName,
        @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal expectedValue,
        @NotNull LeadStage stage,
        @NotNull LeadPriority priority,
        @Size(max = 80) String source,
        LocalDate followUpDate,
        @Size(max = 80) String assignedTo,
        @Size(max = 2000) String notes) {
}

