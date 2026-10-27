package com.salesnav360.service;

import com.salesnav360.dto.LeadRequest;
import com.salesnav360.dto.LeadResponse;
import com.salesnav360.enums.LeadStage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LeadService {

    Page<LeadResponse> search(String query, LeadStage stage, String assignedTo, Pageable pageable);

    LeadResponse get(Long id);

    LeadResponse create(LeadRequest request, String actor);

    LeadResponse update(Long id, LeadRequest request, String actor);

    LeadResponse moveStage(Long id, LeadStage nextStage, String actor);

    LeadResponse convertToClient(Long id, String actor);
}

