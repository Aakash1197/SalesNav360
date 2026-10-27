package com.salesnav360.controller;

import com.salesnav360.dto.LeadRequest;
import com.salesnav360.dto.LeadResponse;
import com.salesnav360.dto.LeadStageRequest;
import com.salesnav360.dto.PageResponse;
import com.salesnav360.enums.LeadStage;
import com.salesnav360.security.SalesNavUserDetails;
import com.salesnav360.service.LeadService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/salesnav/leads")
public class LeadController {

    private final LeadService leadService;

    public LeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    @GetMapping
    public PageResponse<LeadResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) LeadStage stage,
            @RequestParam(required = false) String assignedTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "updatedAt") String sort,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        if (!List.of("fullName", "createdAt", "updatedAt", "expectedValue", "followUpDate", "stage")
                .contains(sort)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Unsupported lead sort field.");
        }
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(direction, sort));
        return PageResponse.from(leadService.search(query, stage, assignedTo, pageable));
    }

    @GetMapping("/{id}")
    public LeadResponse get(@PathVariable Long id) {
        return leadService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeadResponse create(
            @Valid @RequestBody LeadRequest request,
            @AuthenticationPrincipal SalesNavUserDetails user) {
        return leadService.create(request, user.getUsername());
    }

    @PutMapping("/{id}")
    public LeadResponse update(
            @PathVariable Long id,
            @Valid @RequestBody LeadRequest request,
            @AuthenticationPrincipal SalesNavUserDetails user) {
        return leadService.update(id, request, user.getUsername());
    }

    @PatchMapping("/{id}/stage")
    public LeadResponse moveStage(
            @PathVariable Long id,
            @Valid @RequestBody LeadStageRequest request,
            @AuthenticationPrincipal SalesNavUserDetails user) {
        return leadService.moveStage(id, request.stage(), user.getUsername());
    }

    @PostMapping("/{id}/convert")
    public LeadResponse convertToClient(
            @PathVariable Long id,
            @AuthenticationPrincipal SalesNavUserDetails user) {
        return leadService.convertToClient(id, user.getUsername());
    }
}

