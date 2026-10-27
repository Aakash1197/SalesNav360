package com.salesnav360.service;

import com.salesnav360.dto.QuotationRequest;
import com.salesnav360.dto.QuotationResponse;
import com.salesnav360.enums.QuotationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QuotationService {

    Page<QuotationResponse> search(String query, QuotationStatus status, Pageable pageable);

    QuotationResponse get(Long id);

    QuotationResponse create(QuotationRequest request, String actor);

    QuotationResponse update(Long id, QuotationRequest request, String actor);

    QuotationResponse changeStatus(Long id, QuotationStatus status, String actor);
}

