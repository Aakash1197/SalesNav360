package com.salesnav360.service;

import com.salesnav360.dto.PolicyRequest;
import com.salesnav360.dto.PolicyResponse;
import com.salesnav360.enums.PolicyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PolicyService {

    Page<PolicyResponse> search(String query, PolicyStatus status, String assignedTo, Pageable pageable);

    PolicyResponse get(Long id);

    PolicyResponse create(PolicyRequest request, String actor);

    PolicyResponse update(Long id, PolicyRequest request, String actor);
}

