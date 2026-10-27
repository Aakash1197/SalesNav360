package com.salesnav360.service.impl;

import com.salesnav360.dto.PolicyRequest;
import com.salesnav360.dto.PolicyResponse;
import com.salesnav360.dto.PortfolioSummaryResponse;
import com.salesnav360.entity.ClientRecord;
import com.salesnav360.entity.PolicyRecord;
import com.salesnav360.enums.ClientStatus;
import com.salesnav360.enums.PolicyStatus;
import com.salesnav360.repository.ClientRepository;
import com.salesnav360.repository.PolicyRepository;
import com.salesnav360.repository.UserAccountRepository;
import com.salesnav360.service.AuditEventService;
import com.salesnav360.service.PolicyService;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PolicyServiceImpl implements PolicyService {

    private final PolicyRepository policyRepository;
    private final ClientRepository clientRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditEventService auditEventService;

    public PolicyServiceImpl(
            PolicyRepository policyRepository,
            ClientRepository clientRepository,
            UserAccountRepository userAccountRepository,
            AuditEventService auditEventService) {
        this.policyRepository = policyRepository;
        this.clientRepository = clientRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditEventService = auditEventService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PolicyResponse> search(String query, PolicyStatus status, String assignedTo, Pageable pageable) {
        return policyRepository.search(clean(query), status, clean(assignedTo), pageable).map(PolicyResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public PolicyResponse get(Long id) {
        return PolicyResponse.from(find(id));
    }

    @Override
    @Transactional
    public PolicyResponse create(PolicyRequest request, String actor) {
        validate(request, null);
        ClientRecord client = findClient(request.clientId());
        PolicyRecord policy = policyRepository.save(new PolicyRecord(request, client, actor));
        auditEventService.record(actor, "POLICY_CREATED", "POLICY", policy.getId(),
                "Created policy record " + policy.getId());
        return PolicyResponse.from(policy);
    }

    @Override
    @Transactional
    public PolicyResponse update(Long id, PolicyRequest request, String actor) {
        PolicyRecord policy = find(id);
        validate(request, id);
        ClientRecord client = findClient(request.clientId());
        policy.update(request, client);
        auditEventService.record(actor, "POLICY_UPDATED", "POLICY", id, "Updated policy record " + id);
        return PolicyResponse.from(policy);
    }

    @Override
    @Transactional(readOnly = true)
    public PortfolioSummaryResponse portfolioSummary() {
        LocalDate today = LocalDate.now();
        return new PortfolioSummaryResponse(
                policyRepository.count(),
                policyRepository.countByStatus(PolicyStatus.ACTIVE),
                policyRepository.countByStatus(PolicyStatus.PENDING),
                policyRepository.countByStatus(PolicyStatus.EXPIRED),
                policyRepository.countRenewingBetween(PolicyStatus.ACTIVE, today, today.plusDays(30)),
                policyRepository.sumPremiumByStatus(PolicyStatus.ACTIVE));
    }

    private void validate(PolicyRequest request, Long existingId) {
        if (request.startDate().isAfter(request.expiryDate())
                || (request.renewalDate() != null
                    && (request.renewalDate().isBefore(request.startDate())
                        || request.renewalDate().isAfter(request.expiryDate())))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Policy dates must be ordered start, renewal, expiry.");
        }
        policyRepository.findByPolicyNumberIgnoreCase(request.policyNumber().trim())
                .filter(existing -> !Objects.equals(existing.getId(), existingId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "The policy number is already in use.");
                });
        if (request.assignedTo() != null && !request.assignedTo().isBlank()
                && !userAccountRepository.existsByUsernameAndEnabledTrue(request.assignedTo().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Assigned representative must be an active SalesNav360 user.");
        }
    }

    private ClientRecord findClient(Long id) {
        return clientRepository.findByIdAndStatusNot(id, ClientStatus.ARCHIVED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Policy must be linked to an active client or investor."));
    }

    private PolicyRecord find(Long id) {
        return policyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Policy was not found."));
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
