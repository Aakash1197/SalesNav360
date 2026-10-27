package com.salesnav360.entity;

import com.salesnav360.dto.CommunicationRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "sn_communication_history")
public class CommunicationHistoryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_email", nullable = false, length = 255)
    private String recipientEmail;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false, length = 10000)
    private String body;

    @Column(nullable = false, length = 24)
    private String status = "NOT_SENT";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private EmailTemplateRecord template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private ClientRecord client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private LeadRecord lead;

    @Column(name = "created_by", nullable = false, length = 80)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private long version;

    protected CommunicationHistoryRecord() {
    }

    public CommunicationHistoryRecord(
            CommunicationRequest request,
            EmailTemplateRecord template,
            ClientRecord client,
            LeadRecord lead,
            String actor) {
        recipientEmail = request.recipientEmail().trim();
        subject = request.subject().trim();
        body = request.body().trim();
        this.template = template;
        this.client = client;
        this.lead = lead;
        createdBy = actor;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getRecipientEmail() { return recipientEmail; }
    public String getSubject() { return subject; }
    public String getStatus() { return status; }
    public ClientRecord getClient() { return client; }
    public LeadRecord getLead() { return lead; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
