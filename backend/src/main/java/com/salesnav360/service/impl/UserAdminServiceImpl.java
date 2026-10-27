package com.salesnav360.service.impl;

import com.salesnav360.dto.PageResponse;
import com.salesnav360.dto.RoleCreateRequest;
import com.salesnav360.dto.UserCreateRequest;
import com.salesnav360.dto.UserResponse;
import com.salesnav360.dto.UserUpdateRequest;
import com.salesnav360.entity.Role;
import com.salesnav360.entity.UserAccount;
import com.salesnav360.repository.RoleRepository;
import com.salesnav360.repository.UserAccountRepository;
import com.salesnav360.service.AuditEventService;
import com.salesnav360.service.UserAdminService;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserAdminServiceImpl implements UserAdminService {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final Set<String> CREATABLE_ROLES =
            Set.of("SALES_REPRESENTATIVE", "SALES_MANAGER", "OPERATIONS", "READ_ONLY");

    private final UserAccountRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditEventService auditEventService;

    public UserAdminServiceImpl(
            UserAccountRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuditEventService auditEventService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditEventService = auditEventService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> searchUsers(String query, Pageable pageable) {
        Pageable bounded = PageRequest.of(
                Math.max(pageable.getPageNumber(), 0),
                Math.min(Math.max(pageable.getPageSize(), 1), 100),
                Sort.by(Sort.Direction.ASC, "username"));
        String normalized = query == null ? "" : query.trim();
        return PageResponse.from(userRepository
                .findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(normalized, normalized, bounded)
                .map(UserResponse::from));
    }

    @Override
    @Transactional
    public UserResponse createUser(UserCreateRequest request, String actor) {
        String username = request.username().trim();
        if (username.equalsIgnoreCase(actor)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An administrator cannot create a duplicate self account.");
        }
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The username is already in use.");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The email address is already in use.");
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must not exceed 72 UTF-8 bytes.");
        }
        Set<Role> roles = resolveRoles(request.roles());
        UserAccount account = userRepository.save(new UserAccount(
                username,
                email,
                passwordEncoder.encode(request.password()),
                roles));
        auditEventService.record(actor, "USER_CREATED", "USER", account.getId(),
                "Created SalesNav360 user " + account.getUsername());
        return UserResponse.from(account);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request, String actor) {
        UserAccount account = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User was not found."));
        if (account.getUsername().equalsIgnoreCase(actor)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Administrators cannot change their own account access.");
        }

        Set<Role> roles = resolveRoles(request.roles());
        boolean remainsActiveAdmin = account.isEnabled()
                && account.getRoles().stream().anyMatch(role -> ADMIN_ROLE.equals(role.getName()))
                && (!request.enabled() || roles.stream().noneMatch(role -> ADMIN_ROLE.equals(role.getName())));
        if (remainsActiveAdmin && userRepository.countEnabledUsersWithRole(ADMIN_ROLE) <= 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "The last enabled administrator cannot be disabled or demoted.");
        }

        account.setEnabled(request.enabled());
        account.setRoles(roles);
        auditEventService.record(actor, "USER_ACCESS_UPDATED", "USER", account.getId(),
                "Updated access for SalesNav360 user " + account.getUsername());
        return UserResponse.from(account);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<String> listRoles() {
        return roleRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(Role::getName)
                .toList();
    }

    @Override
    @Transactional
    public String createRole(RoleCreateRequest request, String actor) {
        String name = request.name().trim().toUpperCase(Locale.ROOT);
        if (!CREATABLE_ROLES.contains(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The requested role name is not supported.");
        }
        if (roleRepository.findByName(name).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The role already exists.");
        }
        Role role = roleRepository.save(new Role(name));
        auditEventService.record(actor, "ROLE_CREATED", "ROLE", role.getId(), "Created role " + name);
        return name;
    }

    private Set<Role> resolveRoles(Set<String> names) {
        Set<String> normalizedNames = names.stream()
                .map(name -> name.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        Set<Role> roles = normalizedNames.stream()
                .map(name -> roleRepository.findByName(name)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "Role '" + name + "' does not exist.")))
                .collect(Collectors.toUnmodifiableSet());
        if (roles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one role is required.");
        }
        return roles;
    }
}

