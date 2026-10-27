package com.salesnav360.entity;

import com.salesnav360.dto.ClientRequest;
import com.salesnav360.enums.ClientStatus;
import com.salesnav360.enums.ClientType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "sn_clients")
public class ClientRecord {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", nullable = false, length = 20)
    private ClientType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClientStatus status;

    @Column(name = "lead_source", length = 80)
    private String leadSource;

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

    protected ClientRecord() {
    }

    public ClientRecord(ClientRequest request, String actor) {
        update(request);
        this.createdBy = actor;
    }

    public void update(ClientRequest request) {
        this.fullName = request.fullName().trim();
        this.email = clean(request.email());
        this.phone = clean(request.phone());
        this.companyName = clean(request.companyName());
        this.type = request.type();
        this.status = request.status();
        this.leadSource = clean(request.leadSource());
        this.assignedTo = clean(request.assignedTo());
        this.notes = clean(request.notes());
        this.updatedAt = Instant.now();
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
    public ClientType getType() { return type; }
    public ClientStatus getStatus() { return status; }
    public String getLeadSource() { return leadSource; }
    public String getAssignedTo() { return assignedTo; }
    public String getNotes() { return notes; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

