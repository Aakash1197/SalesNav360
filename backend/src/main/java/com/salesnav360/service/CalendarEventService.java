package com.salesnav360.service;

import com.salesnav360.dto.CalendarEventRequest;
import com.salesnav360.dto.CalendarEventResponse;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CalendarEventService {
    Page<CalendarEventResponse> search(LocalDate fromDate, LocalDate toDate, String query, Pageable pageable);
    CalendarEventResponse create(CalendarEventRequest request, String actor);
    CalendarEventResponse update(Long id, CalendarEventRequest request, String actor);
    void delete(Long id, String actor);
}
