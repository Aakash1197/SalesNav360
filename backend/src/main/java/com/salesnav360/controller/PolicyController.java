package com.salesnav360.controller;

import com.salesnav360.dto.PageResponse;
import com.salesnav360.dto.PolicyRequest;
import com.salesnav360.dto.PolicyResponse;
import com.salesnav360.dto.PortfolioSummaryResponse;
import com.salesnav360.enums.PolicyStatus;
import com.salesnav360.security.SalesNavUserDetails;
import com.salesnav360.service.PolicyService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/salesnav/policies")
public class PolicyController {

    private static final List<String> SORT_FIELDS =
            List.of("policyNumber", "policyType", "premiumAmount", "startDate", "expiryDate", "renewalDate", "status");

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_REPRESENTATIVE', 'SALES_MANAGER', 'OPERATIONS')")
    public PageResponse<PolicyResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) PolicyStatus status,
            @RequestParam(required = false) String assignedTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "renewalDate") String sort,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction) {
        if (!SORT_FIELDS.contains(sort)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Unsupported policy sort field.");
        }
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(direction, sort));
        return PageResponse.from(policyService.search(query, status, assignedTo, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_REPRESENTATIVE', 'SALES_MANAGER', 'OPERATIONS')")
    public PolicyResponse get(@PathVariable Long id) {
        return policyService.get(id);
    }

    @GetMapping("/portfolio")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_REPRESENTATIVE', 'SALES_MANAGER', 'OPERATIONS')")
    public PortfolioSummaryResponse portfolioSummary() {
        return policyService.portfolioSummary();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_REPRESENTATIVE', 'SALES_MANAGER', 'OPERATIONS')")
    @ResponseStatus(HttpStatus.CREATED)
    public PolicyResponse create(
            @Valid @RequestBody PolicyRequest request,
            @AuthenticationPrincipal SalesNavUserDetails user) {
        return policyService.create(request, user.getUsername());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_REPRESENTATIVE', 'SALES_MANAGER', 'OPERATIONS')")
    public PolicyResponse update(
            @PathVariable Long id,
            @Valid @RequestBody PolicyRequest request,
            @AuthenticationPrincipal SalesNavUserDetails user) {
        return policyService.update(id, request, user.getUsername());
    }
}
