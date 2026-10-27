package com.salesnav360.service.impl;

import com.salesnav360.dto.ClientRequest;
import com.salesnav360.dto.ClientResponse;
import com.salesnav360.entity.ClientRecord;
import com.salesnav360.enums.ClientStatus;
import com.salesnav360.repository.ClientRepository;
import com.salesnav360.repository.UserAccountRepository;
import com.salesnav360.service.AuditEventService;
import com.salesnav360.service.ClientService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final AuditEventService auditEventService;
    private final UserAccountRepository userAccountRepository;

    public ClientServiceImpl(
            ClientRepository clientRepository,
            AuditEventService auditEventService,
            UserAccountRepository userAccountRepository) {
        this.clientRepository = clientRepository;
        this.auditEventService = auditEventService;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional(readOnly = true)
    public Page<ClientResponse> search(String query, ClientStatus status, String assignedTo, Pageable pageable) {
        return clientRepository.search(clean(query), status, clean(assignedTo), pageable)
                .map(ClientResponse::from);
    }

    @Transactional(readOnly = true)
    public ClientResponse get(Long id) {
        return ClientResponse.from(findActive(id));
    }

    @Transactional
    public ClientResponse create(ClientRequest request, String actor) {
        validateWritableStatus(request);
        validateAssignedTo(request.assignedTo());
        ClientRecord saved = clientRepository.save(new ClientRecord(request, actor));
        auditEventService.record(actor, "CLIENT_CREATED", "CLIENT", saved.getId(),
                "Created " + saved.getType().name().toLowerCase() + " record " + saved.getId());
        return ClientResponse.from(saved);
    }

    @Transactional
    public ClientResponse update(Long id, ClientRequest request, String actor) {
        validateWritableStatus(request);
        validateAssignedTo(request.assignedTo());
        ClientRecord client = findActive(id);
        client.update(request);
        auditEventService.record(actor, "CLIENT_UPDATED", "CLIENT", id, "Updated client record " + id);
        return ClientResponse.from(client);
    }

    @Transactional
    public void archive(Long id, String actor) {
        ClientRecord client = findActive(id);
        ClientRequest archived = new ClientRequest(
                client.getFullName(), client.getEmail(), client.getPhone(), client.getCompanyName(),
                client.getType(), ClientStatus.ARCHIVED, client.getLeadSource(), client.getAssignedTo(),
                client.getNotes());
        client.update(archived);
        auditEventService.record(actor, "CLIENT_ARCHIVED", "CLIENT", id, "Archived client record " + id);
    }

    @Transactional(readOnly = true)
    public List<ClientResponse> findDuplicates(String email, String phone) {
        String normalizedEmail = clean(email);
        String normalizedPhone = clean(phone);
        if (normalizedEmail == null && normalizedPhone == null) {
            return List.of();
        }
        return clientRepository.findPotentialDuplicates(normalizedEmail, normalizedPhone).stream()
                .filter(client -> client.getStatus() != ClientStatus.ARCHIVED)
                .map(ClientResponse::from)
                .toList();
    }

    private ClientRecord findActive(Long id) {
        return clientRepository.findByIdAndStatusNot(id, ClientStatus.ARCHIVED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client was not found."));
    }

    private static void validateWritableStatus(ClientRequest request) {
        if (request.status() == ClientStatus.ARCHIVED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Use the archive action to archive a client record.");
        }
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

