package com.salesnav360.service.impl;

import com.salesnav360.dto.ClientRequest;
import com.salesnav360.dto.LeadRequest;
import com.salesnav360.dto.LeadResponse;
import com.salesnav360.entity.ClientRecord;
import com.salesnav360.entity.LeadRecord;
import com.salesnav360.enums.ClientStatus;
import com.salesnav360.enums.ClientType;
import com.salesnav360.enums.LeadStage;
import com.salesnav360.repository.ClientRepository;
import com.salesnav360.repository.LeadRepository;
import com.salesnav360.repository.UserAccountRepository;
import com.salesnav360.service.AuditEventService;
import com.salesnav360.service.LeadService;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LeadServiceImpl implements LeadService {

    private static final Map<LeadStage, Set<LeadStage>> ALLOWED_TRANSITIONS = Map.of(
            LeadStage.NEW, EnumSet.of(LeadStage.CONTACTED, LeadStage.QUALIFIED, LeadStage.UNQUALIFIED),
            LeadStage.CONTACTED, EnumSet.of(LeadStage.QUALIFIED, LeadStage.PROPOSAL, LeadStage.LOST, LeadStage.UNQUALIFIED),
            LeadStage.QUALIFIED, EnumSet.of(LeadStage.CONTACTED, LeadStage.PROPOSAL, LeadStage.NEGOTIATION,
                    LeadStage.LOST, LeadStage.UNQUALIFIED),
            LeadStage.PROPOSAL, EnumSet.of(LeadStage.NEGOTIATION, LeadStage.WON, LeadStage.LOST),
            LeadStage.NEGOTIATION, EnumSet.of(LeadStage.PROPOSAL, LeadStage.WON, LeadStage.LOST),
            LeadStage.WON, Set.of(),
            LeadStage.LOST, Set.of(),
            LeadStage.UNQUALIFIED, Set.of());

    private final LeadRepository leadRepository;
    private final ClientRepository clientRepository;
    private final AuditEventService auditEventService;
    private final UserAccountRepository userAccountRepository;

    public LeadServiceImpl(
            LeadRepository leadRepository,
            ClientRepository clientRepository,
            AuditEventService auditEventService,
            UserAccountRepository userAccountRepository) {
        this.leadRepository = leadRepository;
        this.clientRepository = clientRepository;
        this.auditEventService = auditEventService;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional(readOnly = true)
    public Page<LeadResponse> search(String query, LeadStage stage, String assignedTo, Pageable pageable) {
        return leadRepository.search(clean(query), stage, clean(assignedTo), pageable).map(LeadResponse::from);
    }

    @Transactional(readOnly = true)
    public LeadResponse get(Long id) {
        return LeadResponse.from(find(id));
    }

    @Transactional
    public LeadResponse create(LeadRequest request, String actor) {
        if (request.stage() != LeadStage.NEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "New leads must start in the NEW stage.");
        }
        validateAssignedTo(request.assignedTo());
        LeadRecord lead = leadRepository.save(new LeadRecord(request, actor));
        auditEventService.record(actor, "LEAD_CREATED", "LEAD", lead.getId(), "Created lead record " + lead.getId());
        return LeadResponse.from(lead);
    }

    @Transactional
    public LeadResponse update(Long id, LeadRequest request, String actor) {
        LeadRecord lead = find(id);
        validateAssignedTo(request.assignedTo());
        if (lead.getStage() != request.stage() && !ALLOWED_TRANSITIONS.get(lead.getStage()).contains(request.stage())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The requested lead stage transition is not allowed.");
        }
        lead.update(request);
        auditEventService.record(actor, "LEAD_UPDATED", "LEAD", id, "Updated lead record " + id);
        return LeadResponse.from(lead);
    }

    @Transactional
    public LeadResponse moveStage(Long id, LeadStage nextStage, String actor) {
        LeadRecord lead = find(id);
        if (lead.getStage() != nextStage && !ALLOWED_TRANSITIONS.get(lead.getStage()).contains(nextStage)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The requested lead stage transition is not allowed.");
        }
        LeadStage previous = lead.getStage();
        lead.moveTo(nextStage);
        auditEventService.record(actor, "LEAD_STAGE_CHANGED", "LEAD", id,
                "Moved lead " + id + " from " + previous + " to " + nextStage);
        return LeadResponse.from(lead);
    }

    @Override
    @Transactional
    public LeadResponse convertToClient(Long id, String actor) {
        LeadRecord lead = find(id);
        if (lead.getConvertedClientId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This lead has already been converted.");
        }
        if (lead.getStage() != LeadStage.QUALIFIED && lead.getStage() != LeadStage.PROPOSAL) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Only qualified leads or leads with a proposal can be converted.");
        }

        boolean hasDuplicate = clientRepository.findPotentialDuplicates(lead.getEmail(), lead.getPhone()).stream()
                .anyMatch(client -> client.getStatus() != ClientStatus.ARCHIVED);
        if (hasDuplicate) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "A client with matching contact details already exists; review it before conversion.");
        }

        ClientRequest clientRequest = new ClientRequest(
                lead.getFullName(),
                lead.getEmail(),
                lead.getPhone(),
                lead.getCompanyName(),
                ClientType.CLIENT,
                ClientStatus.PROSPECT,
                lead.getSource(),
                lead.getAssignedTo(),
                lead.getNotes());
        ClientRecord client = clientRepository.save(new ClientRecord(clientRequest, actor));
        lead.setConvertedClientId(client.getId());
        auditEventService.record(actor, "LEAD_CONVERTED", "LEAD", id,
                "Converted lead " + id + " to client " + client.getId());
        auditEventService.record(actor, "CLIENT_CREATED_FROM_LEAD", "CLIENT", client.getId(),
                "Created client from lead " + id);
        return LeadResponse.from(lead);
    }

    private LeadRecord find(Long id) {
        return leadRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead was not found."));
    }

    private void validateAssignedTo(String username) {
        if (username != null && !username.isBlank()
                && !userAccountRepository.existsByUsernameAndEnabledTrue(username.trim())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Assigned representative must be an active SalesNav360 user.");
        }
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

