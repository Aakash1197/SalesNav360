package com.salesnav360.dto;

import com.salesnav360.enums.LeadStage;
import jakarta.validation.constraints.NotNull;

public record LeadStageRequest(@NotNull LeadStage stage) {
}

