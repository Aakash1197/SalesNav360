package com.salesnav360.controller;

import com.salesnav360.dto.PageResponse;
import com.salesnav360.dto.QuotationRequest;
import com.salesnav360.dto.QuotationResponse;
import com.salesnav360.dto.QuotationStatusRequest;
import com.salesnav360.enums.QuotationStatus;
import com.salesnav360.pdf.QuotationPdfService;
import com.salesnav360.security.SalesNavUserDetails;
import com.salesnav360.service.QuotationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/salesnav/quotations")
@PreAuthorize("hasAnyRole('ADMIN', 'SALES_REPRESENTATIVE', 'SALES_MANAGER', 'OPERATIONS')")
public class QuotationController {

    private static final List<String> SORT_FIELDS =
            List.of("quotationNumber", "validUntil", "createdAt", "updatedAt", "grandTotal", "status");

    private final QuotationService quotationService;
    private final QuotationPdfService quotationPdfService;

    public QuotationController(QuotationService quotationService, QuotationPdfService quotationPdfService) {
        this.quotationService = quotationService;
        this.quotationPdfService = quotationPdfService;
    }

    @GetMapping
    public PageResponse<QuotationResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) QuotationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        if (!SORT_FIELDS.contains(sort)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Unsupported quotation sort field.");
        }
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(direction, sort));
        return PageResponse.from(quotationService.search(query, status, pageable));
    }

    @GetMapping("/{id}")
    public QuotationResponse get(@PathVariable Long id) {
        return quotationService.get(id);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        QuotationResponse quotation = quotationService.get(id);
        byte[] pdf = quotationPdfService.generate(quotation);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + quotation.quotationNumber() + ".pdf\"")
                .body(pdf);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuotationResponse create(
            @Valid @RequestBody QuotationRequest request,
            @AuthenticationPrincipal SalesNavUserDetails user) {
        return quotationService.create(request, user.getUsername());
    }

    @PutMapping("/{id}")
    public QuotationResponse update(
            @PathVariable Long id,
            @Valid @RequestBody QuotationRequest request,
            @AuthenticationPrincipal SalesNavUserDetails user) {
        return quotationService.update(id, request, user.getUsername());
    }

    @PatchMapping("/{id}/status")
    public QuotationResponse changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody QuotationStatusRequest request,
            @AuthenticationPrincipal SalesNavUserDetails user) {
        return quotationService.changeStatus(id, request.status(), user.getUsername());
    }
}
