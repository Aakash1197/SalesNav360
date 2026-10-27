package com.salesnav360.security;

import com.salesnav360.entity.Role;
import com.salesnav360.entity.UserAccount;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class SalesNavUserDetails implements UserDetails {

    private final Long id;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final boolean enabled;
    private final Set<String> roles;

    private SalesNavUserDetails(UserAccount account) {
        this.id = account.getId();
        this.username = account.getUsername();
        this.email = account.getEmail();
        this.passwordHash = account.getPasswordHash();
        this.enabled = account.isEnabled();
        this.roles = account.getRoles().stream().map(Role::getName).collect(Collectors.toUnmodifiableSet());
    }

    public static SalesNavUserDetails from(UserAccount account) {
        return new SalesNavUserDetails(account);
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public Set<String> getRoles() {
        return roles;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}

