package com.salesnav360.entity;

import com.salesnav360.dto.TaskRequest;
import com.salesnav360.enums.TaskPriority;
import com.salesnav360.enums.TaskRelatedEntityType;
import com.salesnav360.enums.TaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "sn_tasks")
public class TaskRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 140)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TaskPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TaskStatus status;

    @Column(name = "assigned_to", length = 80)
    private String assignedTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "related_entity_type", length = 16)
    private TaskRelatedEntityType relatedEntityType;

    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    @Column(name = "created_by", nullable = false, length = 80)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    private long version;

    protected TaskRecord() {
    }

    public TaskRecord(TaskRequest request, String actor) {
        update(request);
        status = TaskStatus.OPEN;
        createdBy = actor;
    }

    public void update(TaskRequest request) {
        title = request.title().trim();
        description = clean(request.description());
        dueDate = request.dueDate();
        priority = request.priority();
        assignedTo = clean(request.assignedTo());
        relatedEntityType = request.relatedEntityType();
        relatedEntityId = request.relatedEntityId();
        updatedAt = Instant.now();
    }

    public void changeStatus(TaskStatus nextStatus) {
        status = nextStatus;
        completedAt = nextStatus == TaskStatus.COMPLETED ? Instant.now() : null;
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
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDate getDueDate() { return dueDate; }
    public TaskPriority getPriority() { return priority; }
    public TaskStatus getStatus() { return status; }
    public String getAssignedTo() { return assignedTo; }
    public TaskRelatedEntityType getRelatedEntityType() { return relatedEntityType; }
    public Long getRelatedEntityId() { return relatedEntityId; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getCompletedAt() { return completedAt; }
}

