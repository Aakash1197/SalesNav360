package com.salesnav360.entity;

import com.salesnav360.dto.EmailTemplateRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "sn_email_templates")
public class EmailTemplateRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false, length = 10000)
    private String body;

    @Column(name = "created_by", nullable = false, length = 80, updatable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected EmailTemplateRecord() {
    }

    public EmailTemplateRecord(EmailTemplateRequest request, String actor) {
        update(request);
        createdBy = actor;
    }

    public void update(EmailTemplateRequest request) {
        name = request.name().trim();
        subject = request.subject().trim();
        body = request.body().trim();
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
    public String getName() { return name; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public String getCreatedBy() { return createdBy; }
    public Instant getUpdatedAt() { return updatedAt; }
}
