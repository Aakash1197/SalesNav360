package com.salesnav360.service.impl;

import com.salesnav360.dto.QuotationRequest;
import com.salesnav360.dto.QuotationResponse;
import com.salesnav360.entity.ClientRecord;
import com.salesnav360.entity.LeadRecord;
import com.salesnav360.entity.QuotationRecord;
import com.salesnav360.enums.ClientStatus;
import com.salesnav360.enums.QuotationStatus;
import com.salesnav360.repository.ClientRepository;
import com.salesnav360.repository.LeadRepository;
import com.salesnav360.repository.QuotationRepository;
import com.salesnav360.service.AuditEventService;
import com.salesnav360.service.QuotationService;
import com.salesnav360.utilis.QuotationTotalsCalculator;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class QuotationServiceImpl implements QuotationService {

    private static final Map<QuotationStatus, Set<QuotationStatus>> ALLOWED_TRANSITIONS = Map.of(
            QuotationStatus.DRAFT, EnumSet.of(QuotationStatus.SENT),
            QuotationStatus.SENT, EnumSet.of(QuotationStatus.ACCEPTED, QuotationStatus.REJECTED, QuotationStatus.EXPIRED),
            QuotationStatus.ACCEPTED, Set.of(),
            QuotationStatus.REJECTED, Set.of(),
            QuotationStatus.EXPIRED, Set.of());

    private final QuotationRepository quotationRepository;
    private final ClientRepository clientRepository;
    private final LeadRepository leadRepository;
    private final AuditEventService auditEventService;

    public QuotationServiceImpl(
            QuotationRepository quotationRepository,
            ClientRepository clientRepository,
            LeadRepository leadRepository,
            AuditEventService auditEventService) {
        this.quotationRepository = quotationRepository;
        this.clientRepository = clientRepository;
        this.leadRepository = leadRepository;
        this.auditEventService = auditEventService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuotationResponse> search(String query, QuotationStatus status, Pageable pageable) {
        String normalized = query == null || query.isBlank() ? null : query.trim();
        return quotationRepository.search(normalized, status, pageable).map(QuotationResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public QuotationResponse get(Long id) {
        return QuotationResponse.from(find(id));
    }

    @Override
    @Transactional
    public QuotationResponse create(QuotationRequest request, String actor) {
        validateRequest(request);
        ClientRecord client = findClient(request.clientId());
        LeadRecord lead = findLead(request.leadId(), client);
        String number = "SNQ-" + LocalDate.now().getYear() + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        QuotationRecord quotation = new QuotationRecord(number, request, client, lead, actor);
        updateTotals(quotation, request);
        QuotationRecord saved = quotationRepository.save(quotation);
        auditEventService.record(actor, "QUOTATION_CREATED", "QUOTATION", saved.getId(),
                "Created quotation " + saved.getQuotationNumber());
        return QuotationResponse.from(saved);
    }

    @Override
    @Transactional
    public QuotationResponse update(Long id, QuotationRequest request, String actor) {
        QuotationRecord quotation = find(id);
        if (quotation.getStatus() != QuotationStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only draft quotations can be edited.");
        }
        validateRequest(request);
        ClientRecord client = findClient(request.clientId());
        LeadRecord lead = findLead(request.leadId(), client);
        quotation.update(request, client, lead);
        updateTotals(quotation, request);
        auditEventService.record(actor, "QUOTATION_UPDATED", "QUOTATION", id,
                "Updated quotation " + quotation.getQuotationNumber());
        return QuotationResponse.from(quotation);
    }

    @Override
    @Transactional
    public QuotationResponse changeStatus(Long id, QuotationStatus nextStatus, String actor) {
        QuotationRecord quotation = find(id);
        if (quotation.getStatus() != nextStatus
                && !ALLOWED_TRANSITIONS.get(quotation.getStatus()).contains(nextStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "The requested quotation status transition is not allowed.");
        }
        QuotationStatus previous = quotation.getStatus();
        quotation.changeStatus(nextStatus);
        auditEventService.record(actor, "QUOTATION_STATUS_CHANGED", "QUOTATION", id,
                "Changed quotation " + quotation.getQuotationNumber() + " from " + previous + " to " + nextStatus);
        return QuotationResponse.from(quotation);
    }

    private void validateRequest(QuotationRequest request) {
        if (request.validUntil().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quotation validity must be today or later.");
        }
    }

    private ClientRecord findClient(Long id) {
        return clientRepository.findByIdAndStatusNot(id, ClientStatus.ARCHIVED)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Quotation must be linked to an active client or investor."));
    }

    private LeadRecord findLead(Long id, ClientRecord client) {
        if (id == null) return null;
        LeadRecord lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "The linked lead was not found."));
        if (lead.getConvertedClientId() != null && !lead.getConvertedClientId().equals(client.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The linked lead belongs to another client.");
        }
        return lead;
    }

    private static void updateTotals(QuotationRecord quotation, QuotationRequest request) {
        QuotationTotalsCalculator.Totals totals = QuotationTotalsCalculator.calculate(request);
        quotation.updateTotals(totals.subtotal(), totals.discount(), totals.tax(), totals.grandTotal());
    }

    private QuotationRecord find(Long id) {
        return quotationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quotation was not found."));
    }
}
