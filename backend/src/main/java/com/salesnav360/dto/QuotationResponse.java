package com.salesnav360.dto;

import com.salesnav360.entity.QuotationRecord;
import com.salesnav360.enums.QuotationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record QuotationResponse(
        Long id,
        String quotationNumber,
        Long clientId,
        String clientName,
        Long leadId,
        LocalDate validUntil,
        BigDecimal discountPercent,
        BigDecimal taxPercent,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal tax,
        BigDecimal grandTotal,
        QuotationStatus status,
        List<QuotationLineItemResponse> lineItems,
        Instant createdAt,
        Instant updatedAt) {

    public static QuotationResponse from(QuotationRecord quotation) {
        return new QuotationResponse(
                quotation.getId(),
                quotation.getQuotationNumber(),
                quotation.getClient().getId(),
                quotation.getClient().getFullName(),
                quotation.getLead() == null ? null : quotation.getLead().getId(),
                quotation.getValidUntil(),
                quotation.getDiscountPercent(),
                quotation.getTaxPercent(),
                quotation.getSubtotal(),
                quotation.getDiscount(),
                quotation.getTax(),
                quotation.getGrandTotal(),
                quotation.getStatus(),
                quotation.getLineItems().stream().map(QuotationLineItemResponse::from).toList(),
                quotation.getCreatedAt(),
                quotation.getUpdatedAt());
    }
}

