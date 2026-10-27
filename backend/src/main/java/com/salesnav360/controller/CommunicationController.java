package com.salesnav360.controller;

import com.salesnav360.dto.CommunicationRequest;
import com.salesnav360.dto.CommunicationResponse;
import com.salesnav360.dto.EmailTemplateRequest;
import com.salesnav360.dto.EmailTemplateResponse;
import com.salesnav360.dto.PageResponse;
import com.salesnav360.security.SalesNavUserDetails;
import com.salesnav360.service.CommunicationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/v1/salesnav/communications")
public class CommunicationController {

    private final CommunicationService communicationService;

    public CommunicationController(CommunicationService communicationService) {
        this.communicationService = communicationService;
    }

    @GetMapping("/templates")
    public PageResponse<EmailTemplateResponse> templates(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.ASC, "name"));
        return PageResponse.from(communicationService.templates(query, pageable));
    }

    @PostMapping("/templates")
    @ResponseStatus(HttpStatus.CREATED)
    public EmailTemplateResponse createTemplate(
            @Valid @RequestBody EmailTemplateRequest request,
            @AuthenticationPrincipal SalesNavUserDetails user) {
        return communicationService.createTemplate(request, user.getUsername());
    }

    @PutMapping("/templates/{id}")
    public EmailTemplateResponse updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody EmailTemplateRequest request,
            @AuthenticationPrincipal SalesNavUserDetails user) {
        return communicationService.updateTemplate(id, request, user.getUsername());
    }

    @GetMapping("/history")
    public PageResponse<CommunicationResponse> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(communicationService.history(pageable));
    }

    @PostMapping("/history")
    @ResponseStatus(HttpStatus.CREATED)
    public CommunicationResponse logCommunication(
            @Valid @RequestBody CommunicationRequest request,
            @AuthenticationPrincipal SalesNavUserDetails user) {
        return communicationService.logCommunication(request, user.getUsername());
    }
}
