package com.salesnav360.dto;

import com.salesnav360.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record TaskStatusRequest(@NotNull TaskStatus status) {
}

