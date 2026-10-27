package com.salesnav360.entity;

import com.salesnav360.dto.PolicyRequest;
import com.salesnav360.enums.PolicyStatus;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "sn_policies")
public class PolicyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_number", nullable = false, unique = true, length = 80)
    private String policyNumber;

    @Column(name = "policy_type", nullable = false, length = 80)
    private String policyType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientRecord client;

    @Column(name = "provider", nullable = false, length = 160)
    private String provider;

    @Column(name = "premium_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal premiumAmount;

    @Column(name = "coverage_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal coverageAmount;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "renewal_date")
    private LocalDate renewalDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PolicyStatus status;

    @Column(name = "assigned_to", length = 80)
    private String assignedTo;

    @Column(name = "created_by", nullable = false, length = 80)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected PolicyRecord() {
    }

    public PolicyRecord(PolicyRequest request, ClientRecord client, String actor) {
        update(request, client);
        createdBy = actor;
    }

    public void update(PolicyRequest request, ClientRecord client) {
        policyNumber = request.policyNumber().trim();
        policyType = request.policyType().trim();
        this.client = client;
        provider = request.provider().trim();
        premiumAmount = request.premiumAmount();
        coverageAmount = request.coverageAmount();
        startDate = request.startDate();
        expiryDate = request.expiryDate();
        renewalDate = request.renewalDate();
        status = request.status();
        assignedTo = clean(request.assignedTo());
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

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public Long getId() { return id; }
    public String getPolicyNumber() { return policyNumber; }
    public String getPolicyType() { return policyType; }
    public ClientRecord getClient() { return client; }
    public String getProvider() { return provider; }
    public BigDecimal getPremiumAmount() { return premiumAmount; }
    public BigDecimal getCoverageAmount() { return coverageAmount; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public LocalDate getRenewalDate() { return renewalDate; }
    public PolicyStatus getStatus() { return status; }
    public String getAssignedTo() { return assignedTo; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

