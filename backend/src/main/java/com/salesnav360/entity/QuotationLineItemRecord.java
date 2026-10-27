package com.salesnav360.entity;

import com.salesnav360.dto.QuotationLineItemRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "sn_quotation_items")
public class QuotationLineItemRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quotation_id", nullable = false)
    private QuotationRecord quotation;

    @Column(nullable = false, length = 160)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    protected QuotationLineItemRecord() {
    }

    public QuotationLineItemRecord(QuotationRecord quotation, QuotationLineItemRequest request) {
        this.quotation = quotation;
        this.description = request.description().trim();
        this.quantity = request.quantity();
        this.unitPrice = request.unitPrice();
    }

    public String getDescription() { return description; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
}

