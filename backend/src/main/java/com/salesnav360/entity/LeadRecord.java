package com.salesnav360.entity;

import com.salesnav360.dto.LeadRequest;
import com.salesnav360.enums.LeadPriority;
import com.salesnav360.enums.LeadStage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "sn_leads")
public class LeadRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(length = 255)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(name = "company_name", length = 160)
    private String companyName;

    @Column(name = "expected_value", precision = 19, scale = 2)
    private BigDecimal expectedValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeadStage stage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeadPriority priority;

    @Column(length = 80)
    private String source;

    @Column(name = "follow_up_date")
    private LocalDate followUpDate;

    @Column(name = "assigned_to", length = 80)
    private String assignedTo;

    @Column(length = 2000)
    private String notes;

    @Column(name = "created_by", nullable = false, length = 80)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "converted_client_id")
    private Long convertedClientId;

    @Version
    private long version;

    protected LeadRecord() {
    }

    public LeadRecord(LeadRequest request, String actor) {
        update(request);
        createdBy = actor;
    }

    public void update(LeadRequest request) {
        fullName = request.fullName().trim();
        email = clean(request.email());
        phone = clean(request.phone());
        companyName = clean(request.companyName());
        expectedValue = request.expectedValue();
        stage = request.stage();
        priority = request.priority();
        source = clean(request.source());
        followUpDate = request.followUpDate();
        assignedTo = clean(request.assignedTo());
        notes = clean(request.notes());
        updatedAt = Instant.now();
    }

    public void moveTo(LeadStage nextStage) {
        stage = nextStage;
        updatedAt = Instant.now();
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }
    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getCompanyName() { return companyName; }
    public BigDecimal getExpectedValue() { return expectedValue; }
    public LeadStage getStage() { return stage; }
    public LeadPriority getPriority() { return priority; }
    public String getSource() { return source; }
    public LocalDate getFollowUpDate() { return followUpDate; }
    public String getAssignedTo() { return assignedTo; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getConvertedClientId() { return convertedClientId; }
    public void setConvertedClientId(Long convertedClientId) { this.convertedClientId = convertedClientId; }
}

