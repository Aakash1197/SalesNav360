package com.salesnav360.controller;

import com.salesnav360.dto.CalendarEventRequest;
import com.salesnav360.dto.CalendarEventResponse;
import com.salesnav360.dto.PageResponse;
import com.salesnav360.security.SalesNavUserDetails;
import com.salesnav360.service.CalendarEventService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/salesnav/calendar/events")
public class CalendarEventController {

    private static final List<String> SORT_FIELDS = List.of("eventDate", "startTime", "title", "createdAt");
    private final CalendarEventService calendarEventService;

    public CalendarEventController(CalendarEventService calendarEventService) {
        this.calendarEventService = calendarEventService;
    }

    @GetMapping
    public PageResponse<CalendarEventResponse> search(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "eventDate") String sort,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction) {
        if (!SORT_FIELDS.contains(sort)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Unsupported calendar sort field.");
        }
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(direction, sort));
        return PageResponse.from(calendarEventService.search(fromDate, toDate, query, pageable));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CalendarEventResponse create(
            @Valid @RequestBody CalendarEventRequest request,
            @AuthenticationPrincipal SalesNavUserDetails user) {
        return calendarEventService.create(request, user.getUsername());
    }

    @PutMapping("/{id}")
    public CalendarEventResponse update(
            @PathVariable Long id,
            @Valid @RequestBody CalendarEventRequest request,
            @AuthenticationPrincipal SalesNavUserDetails user) {
        return calendarEventService.update(id, request, user.getUsername());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal SalesNavUserDetails user) {
        calendarEventService.delete(id, user.getUsername());
    }
}
