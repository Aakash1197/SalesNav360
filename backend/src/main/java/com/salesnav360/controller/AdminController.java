package com.salesnav360.controller;

import com.salesnav360.dto.ActivityResponse;
import com.salesnav360.dto.PageResponse;
import com.salesnav360.dto.RoleCreateRequest;
import com.salesnav360.dto.UserCreateRequest;
import com.salesnav360.dto.UserResponse;
import com.salesnav360.dto.UserUpdateRequest;
import com.salesnav360.security.SalesNavUserDetails;
import com.salesnav360.service.AuditEventService;
import com.salesnav360.service.UserAdminService;
import jakarta.validation.Valid;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/salesnav/admin")
public class AdminController {

    private final UserAdminService userAdminService;
    private final AuditEventService auditEventService;

    public AdminController(UserAdminService userAdminService, AuditEventService auditEventService) {
        this.userAdminService = userAdminService;
        this.auditEventService = auditEventService;
    }

    @GetMapping("/access")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminAccessResponse verifyAdminAccess(@AuthenticationPrincipal SalesNavUserDetails user) {
        return new AdminAccessResponse(user.getId(), user.getUsername(), user.getRoles());
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<UserResponse> users(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return userAdminService.searchUsers(query, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.ASC, "username")));
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(
            @Valid @RequestBody UserCreateRequest request,
            @AuthenticationPrincipal SalesNavUserDetails actor) {
        return userAdminService.createUser(request, actor.getUsername());
    }

    @PatchMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request,
            @AuthenticationPrincipal SalesNavUserDetails actor) {
        return userAdminService.updateUser(id, request, actor.getUsername());
    }

    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public Set<String> roles() {
        return Set.copyOf(userAdminService.listRoles());
    }

    @PostMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public String createRole(
            @Valid @RequestBody RoleCreateRequest request,
            @AuthenticationPrincipal SalesNavUserDetails actor) {
        return userAdminService.createRole(request, actor.getUsername());
    }

    @GetMapping("/audit-events")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<ActivityResponse> auditEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return auditEventService.search(PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
    }

    public record AdminAccessResponse(Long userId, String username, Set<String> roles) {
    }
}

