package com.salesnav360.service;

import com.salesnav360.dto.ClientRequest;
import com.salesnav360.dto.ClientResponse;
import com.salesnav360.enums.ClientStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClientService {

    Page<ClientResponse> search(String query, ClientStatus status, String assignedTo, Pageable pageable);

    ClientResponse get(Long id);

    ClientResponse create(ClientRequest request, String actor);

    ClientResponse update(Long id, ClientRequest request, String actor);

    void archive(Long id, String actor);

    List<ClientResponse> findDuplicates(String email, String phone);
}

