package com.salesnav360.utilis;

import com.salesnav360.dto.QuotationRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class QuotationTotalsCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private QuotationTotalsCalculator() {
    }

    public static Totals calculate(QuotationRequest request) {
        BigDecimal subtotal = request.lineItems().stream()
                .map(item -> item.quantity().multiply(item.unitPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount = subtotal.multiply(request.discountPercent())
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal taxable = subtotal.subtract(discount);
        BigDecimal tax = taxable.multiply(request.taxPercent()).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = taxable.add(tax).setScale(2, RoundingMode.HALF_UP);
        return new Totals(subtotal, discount, tax, grandTotal);
    }

    public record Totals(BigDecimal subtotal, BigDecimal discount, BigDecimal tax, BigDecimal grandTotal) {
    }
}
