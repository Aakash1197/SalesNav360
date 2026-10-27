package com.salesnav360.controller;

import com.salesnav360.dto.ClientRequest;
import com.salesnav360.dto.ClientResponse;
import com.salesnav360.dto.PageResponse;
import com.salesnav360.enums.ClientStatus;
import com.salesnav360.security.SalesNavUserDetails;
import com.salesnav360.service.ClientService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/salesnav/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    public PageResponse<ClientResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) ClientStatus status,
            @RequestParam(required = false) String assignedTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sort,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        if (!List.of("fullName", "createdAt", "updatedAt", "status").contains(sort)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Unsupported client sort field.");
        }
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(direction, sort));
        return PageResponse.from(clientService.search(query, status, assignedTo, pageable));
    }

    @GetMapping("/{id}")
    public ClientResponse get(@PathVariable Long id) {
        return clientService.get(id);
    }

    @GetMapping("/duplicates")
    public List<ClientResponse> duplicates(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone) {
        return clientService.findDuplicates(email, phone);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientResponse create(
            @Valid @RequestBody ClientRequest request,
            @AuthenticationPrincipal SalesNavUserDetails user) {
        return clientService.create(request, user.getUsername());
    }

    @PutMapping("/{id}")
    public ClientResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ClientRequest request,
            @AuthenticationPrincipal SalesNavUserDetails user) {
        return clientService.update(id, request, user.getUsername());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable Long id, @AuthenticationPrincipal SalesNavUserDetails user) {
        clientService.archive(id, user.getUsername());
    }
}

