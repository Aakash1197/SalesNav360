package com.salesnav360.service;

import com.salesnav360.dto.CommunicationRequest;
import com.salesnav360.dto.CommunicationResponse;
import com.salesnav360.dto.EmailTemplateRequest;
import com.salesnav360.dto.EmailTemplateResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommunicationService {
    Page<EmailTemplateResponse> templates(String query, Pageable pageable);
    EmailTemplateResponse createTemplate(EmailTemplateRequest request, String actor);
    EmailTemplateResponse updateTemplate(Long id, EmailTemplateRequest request, String actor);
    Page<CommunicationResponse> history(Pageable pageable);
    CommunicationResponse logCommunication(CommunicationRequest request, String actor);
}
