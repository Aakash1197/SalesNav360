package com.salesnav360.service.impl;

import com.salesnav360.dto.CommunicationRequest;
import com.salesnav360.dto.CommunicationResponse;
import com.salesnav360.dto.EmailTemplateRequest;
import com.salesnav360.dto.EmailTemplateResponse;
import com.salesnav360.entity.ClientRecord;
import com.salesnav360.entity.CommunicationHistoryRecord;
import com.salesnav360.entity.EmailTemplateRecord;
import com.salesnav360.entity.LeadRecord;
import com.salesnav360.enums.ClientStatus;
import com.salesnav360.repository.ClientRepository;
import com.salesnav360.repository.CommunicationHistoryRepository;
import com.salesnav360.repository.EmailTemplateRepository;
import com.salesnav360.repository.LeadRepository;
import com.salesnav360.service.AuditEventService;
import com.salesnav360.service.CommunicationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CommunicationServiceImpl implements CommunicationService {

    private final EmailTemplateRepository templateRepository;
    private final CommunicationHistoryRepository historyRepository;
    private final ClientRepository clientRepository;
    private final LeadRepository leadRepository;
    private final AuditEventService auditEventService;

    public CommunicationServiceImpl(
            EmailTemplateRepository templateRepository,
            CommunicationHistoryRepository historyRepository,
            ClientRepository clientRepository,
            LeadRepository leadRepository,
            AuditEventService auditEventService) {
        this.templateRepository = templateRepository;
        this.historyRepository = historyRepository;
        this.clientRepository = clientRepository;
        this.leadRepository = leadRepository;
        this.auditEventService = auditEventService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmailTemplateResponse> templates(String query, Pageable pageable) {
        String cleanQuery = query == null || query.isBlank() ? null : query.trim();
        return templateRepository.search(cleanQuery, pageable).map(EmailTemplateResponse::from);
    }

    @Override
    @Transactional
    public EmailTemplateResponse createTemplate(EmailTemplateRequest request, String actor) {
        EmailTemplateRecord template = templateRepository.save(new EmailTemplateRecord(request, actor));
        auditEventService.record(actor, "EMAIL_TEMPLATE_CREATED", "EMAIL_TEMPLATE", template.getId(),
                "Created email template " + template.getId());
        return EmailTemplateResponse.from(template);
    }

    @Override
    @Transactional
    public EmailTemplateResponse updateTemplate(Long id, EmailTemplateRequest request, String actor) {
        EmailTemplateRecord template = templateRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Email template was not found."));
        template.update(request);
        auditEventService.record(actor, "EMAIL_TEMPLATE_UPDATED", "EMAIL_TEMPLATE", id,
                "Updated email template " + id);
        return EmailTemplateResponse.from(template);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommunicationResponse> history(Pageable pageable) {
        return historyRepository.findAllByOrderByCreatedAtDesc(pageable).map(CommunicationResponse::from);
    }

    @Override
    @Transactional
    public CommunicationResponse logCommunication(CommunicationRequest request, String actor) {
        EmailTemplateRecord template = request.templateId() == null ? null
                : templateRepository.findById(request.templateId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "The selected email template was not found."));
        ClientRecord client = request.clientId() == null ? null
                : clientRepository.findByIdAndStatusNot(request.clientId(), ClientStatus.ARCHIVED)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "The selected client or investor was not found."));
        LeadRecord lead = request.leadId() == null ? null
                : leadRepository.findById(request.leadId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "The selected lead was not found."));
        CommunicationHistoryRecord communication = historyRepository.save(
                new CommunicationHistoryRecord(request, template, client, lead, actor));
        auditEventService.record(actor, "COMMUNICATION_LOGGED", "COMMUNICATION", communication.getId(),
                "Recorded a communication; no email was sent by this operation.");
        return CommunicationResponse.from(communication);
    }
}
