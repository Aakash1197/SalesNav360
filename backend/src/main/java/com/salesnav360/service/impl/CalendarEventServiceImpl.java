package com.salesnav360.service.impl;

import com.salesnav360.dto.CalendarEventRequest;
import com.salesnav360.dto.CalendarEventResponse;
import com.salesnav360.entity.CalendarEventRecord;
import com.salesnav360.entity.ClientRecord;
import com.salesnav360.enums.ClientStatus;
import com.salesnav360.repository.CalendarEventRepository;
import com.salesnav360.repository.ClientRepository;
import com.salesnav360.service.AuditEventService;
import com.salesnav360.service.CalendarEventService;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CalendarEventServiceImpl implements CalendarEventService {

    private final CalendarEventRepository eventRepository;
    private final ClientRepository clientRepository;
    private final AuditEventService auditEventService;

    public CalendarEventServiceImpl(
            CalendarEventRepository eventRepository,
            ClientRepository clientRepository,
            AuditEventService auditEventService) {
        this.eventRepository = eventRepository;
        this.clientRepository = clientRepository;
        this.auditEventService = auditEventService;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<CalendarEventResponse> search(
            LocalDate fromDate, LocalDate toDate, String query, org.springframework.data.domain.Pageable pageable) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The calendar start date must not follow its end date.");
        }
        String cleanQuery = query == null || query.isBlank() ? null : query.trim();
        return eventRepository.search(fromDate, toDate, cleanQuery, pageable).map(CalendarEventResponse::from);
    }

    @Override
    @Transactional
    public CalendarEventResponse create(CalendarEventRequest request, String actor) {
        validateUnique(request, null);
        CalendarEventRecord event = eventRepository.save(new CalendarEventRecord(request, findClient(request.clientId()), actor));
        auditEventService.record(actor, "CALENDAR_EVENT_CREATED", "CALENDAR_EVENT", event.getId(),
                "Created calendar event " + event.getId());
        return CalendarEventResponse.from(event);
    }

    @Override
    @Transactional
    public CalendarEventResponse update(Long id, CalendarEventRequest request, String actor) {
        CalendarEventRecord event = find(id);
        validateUnique(request, id);
        event.update(request, findClient(request.clientId()));
        auditEventService.record(actor, "CALENDAR_EVENT_UPDATED", "CALENDAR_EVENT", id,
                "Updated calendar event " + id);
        return CalendarEventResponse.from(event);
    }

    @Override
    @Transactional
    public void delete(Long id, String actor) {
        CalendarEventRecord event = find(id);
        eventRepository.delete(event);
        auditEventService.record(actor, "CALENDAR_EVENT_DELETED", "CALENDAR_EVENT", id,
                "Deleted calendar event " + id);
    }

    private ClientRecord findClient(Long clientId) {
        if (clientId == null) return null;
        return clientRepository.findByIdAndStatusNot(clientId, ClientStatus.ARCHIVED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Calendar events must be linked to an active client or investor."));
    }

    private void validateUnique(CalendarEventRequest request, Long excludeId) {
        if (eventRepository.existsDuplicate(
                request.title().trim(), request.eventDate(), request.startTime(), request.clientId(), excludeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A calendar event with the same title, date, time, and client already exists.");
        }
    }

    private CalendarEventRecord find(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Calendar event was not found."));
    }
}
