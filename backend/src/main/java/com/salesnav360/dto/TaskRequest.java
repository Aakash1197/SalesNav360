package com.salesnav360.dto;

import com.salesnav360.enums.TaskPriority;
import com.salesnav360.enums.TaskRelatedEntityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TaskRequest(
        @NotBlank @Size(max = 140) String title,
        @Size(max = 2000) String description,
        LocalDate dueDate,
        @NotNull TaskPriority priority,
        @Size(max = 80) String assignedTo,
        TaskRelatedEntityType relatedEntityType,
        Long relatedEntityId) {
}

