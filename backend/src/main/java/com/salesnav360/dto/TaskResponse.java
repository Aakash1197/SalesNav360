package com.salesnav360.dto;

import com.salesnav360.entity.TaskRecord;
import com.salesnav360.enums.TaskPriority;
import com.salesnav360.enums.TaskRelatedEntityType;
import com.salesnav360.enums.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;

public record TaskResponse(
        Long id,
        String title,
        String description,
        LocalDate dueDate,
        TaskPriority priority,
        TaskStatus status,
        String assignedTo,
        TaskRelatedEntityType relatedEntityType,
        Long relatedEntityId,
        String createdBy,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt) {

    public static TaskResponse from(TaskRecord task) {
        return new TaskResponse(task.getId(), task.getTitle(), task.getDescription(), task.getDueDate(),
                task.getPriority(), task.getStatus(), task.getAssignedTo(), task.getRelatedEntityType(),
                task.getRelatedEntityId(), task.getCreatedBy(), task.getCreatedAt(), task.getUpdatedAt(),
                task.getCompletedAt());
    }
}

