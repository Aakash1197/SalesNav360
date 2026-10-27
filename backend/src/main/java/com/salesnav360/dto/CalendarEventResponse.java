package com.salesnav360.dto;

import com.salesnav360.entity.CalendarEventRecord;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record CalendarEventResponse(
        Long id,
        String title,
        String type,
        LocalDate eventDate,
        LocalTime startTime,
        LocalTime endTime,
        String description,
        Long clientId,
        String clientName,
        String createdBy,
        Instant createdAt) {

    public static CalendarEventResponse from(CalendarEventRecord event) {
        return new CalendarEventResponse(
                event.getId(),
                event.getTitle(),
                event.getType(),
                event.getEventDate(),
                event.getStartTime(),
                event.getEndTime(),
                event.getDescription(),
                event.getClient() == null ? null : event.getClient().getId(),
                event.getClient() == null ? null : event.getClient().getFullName(),
                event.getCreatedBy(),
                event.getCreatedAt());
    }
}
