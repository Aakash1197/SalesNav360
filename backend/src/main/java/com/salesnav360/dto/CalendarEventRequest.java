package com.salesnav360.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record CalendarEventRequest(
        @NotBlank @Size(max = 140) String title,
        @NotBlank @Size(max = 32) String type,
        @NotNull LocalDate eventDate,
        LocalTime startTime,
        LocalTime endTime,
        @Size(max = 2000) String description,
        Long clientId) {

    @AssertTrue(message = "Event end time must be after its start time.")
    public boolean isTimeRangeValid() {
        return startTime == null || endTime == null || endTime.isAfter(startTime);
    }
}
