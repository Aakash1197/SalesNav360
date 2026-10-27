package com.salesnav360.entity;

import com.salesnav360.dto.QuotationRequest;
import com.salesnav360.enums.QuotationStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sn_quotations")
public class QuotationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quotation_number", nullable = false, unique = true, length = 32)
    private String quotationNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientRecord client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private LeadRecord lead;

    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil;

    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(name = "tax_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxPercent;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal discount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal tax;

    @Column(name = "grand_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal grandTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private QuotationStatus status;

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuotationLineItemRecord> lineItems = new ArrayList<>();

    @Column(name = "created_by", nullable = false, length = 80)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected QuotationRecord() {
    }

    public QuotationRecord(
            String quotationNumber,
            QuotationRequest request,
            ClientRecord client,
            LeadRecord lead,
            String actor) {
        this.quotationNumber = quotationNumber;
        this.createdBy = actor;
        update(request, client, lead);
        status = QuotationStatus.DRAFT;
    }

    public void update(QuotationRequest request, ClientRecord client, LeadRecord lead) {
        this.client = client;
        this.lead = lead;
        validUntil = request.validUntil();
        discountPercent = request.discountPercent();
        taxPercent = request.taxPercent();
        lineItems.clear();
        request.lineItems().forEach(item -> lineItems.add(new QuotationLineItemRecord(this, item)));
        updatedAt = Instant.now();
    }

    public void updateTotals(BigDecimal subtotal, BigDecimal discount, BigDecimal tax, BigDecimal grandTotal) {
        this.subtotal = subtotal;
        this.discount = discount;
        this.tax = tax;
        this.grandTotal = grandTotal;
    }

    public void changeStatus(QuotationStatus status) {
        this.status = status;
        updatedAt = Instant.now();
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getQuotationNumber() { return quotationNumber; }
    public ClientRecord getClient() { return client; }
    public LeadRecord getLead() { return lead; }
    public LocalDate getValidUntil() { return validUntil; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public BigDecimal getTaxPercent() { return taxPercent; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getTax() { return tax; }
    public BigDecimal getGrandTotal() { return grandTotal; }
    public QuotationStatus getStatus() { return status; }
    public List<QuotationLineItemRecord> getLineItems() { return List.copyOf(lineItems); }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

