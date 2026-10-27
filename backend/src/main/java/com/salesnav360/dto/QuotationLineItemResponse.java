package com.salesnav360.dto;

import com.salesnav360.entity.QuotationLineItemRecord;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record QuotationLineItemResponse(
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal) {

    public static QuotationLineItemResponse from(QuotationLineItemRecord item) {
        BigDecimal total = item.getQuantity().multiply(item.getUnitPrice()).setScale(2, RoundingMode.HALF_UP);
        return new QuotationLineItemResponse(item.getDescription(), item.getQuantity(), item.getUnitPrice(), total);
    }
}

