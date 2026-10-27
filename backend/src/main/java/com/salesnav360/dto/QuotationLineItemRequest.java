package com.salesnav360.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record QuotationLineItemRequest(
        @NotBlank @Size(max = 160) String description,
        @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal quantity,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal unitPrice) {
}

