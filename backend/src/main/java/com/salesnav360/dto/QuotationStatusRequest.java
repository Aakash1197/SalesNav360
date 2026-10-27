package com.salesnav360.dto;

import com.salesnav360.enums.QuotationStatus;
import jakarta.validation.constraints.NotNull;

public record QuotationStatusRequest(@NotNull QuotationStatus status) {
}

