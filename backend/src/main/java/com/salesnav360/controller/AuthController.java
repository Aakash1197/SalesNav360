package com.salesnav360.controller;

import com.salesnav360.dto.AuthResponse;
import com.salesnav360.dto.CsrfResponse;
import com.salesnav360.dto.LoginRequest;
import com.salesnav360.security.SalesNavJwtService;
import com.salesnav360.security.SalesNavUserDetails;
import com.salesnav360.service.AuditEventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/salesnav/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SalesNavJwtService jwtService;
    private final CsrfTokenRepository csrfTokenRepository;
    private final AuditEventService auditEventService;
    private final String cookieName;
    private final boolean cookieSecure;

    public AuthController(
            AuthenticationManager authenticationManager,
            SalesNavJwtService jwtService,
            CsrfTokenRepository csrfTokenRepository,
            AuditEventService auditEventService,
            @Value("${salesnav.auth.cookie-name}") String cookieName,
            @Value("${salesnav.auth.cookie-secure}") boolean cookieSecure) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.csrfTokenRepository = csrfTokenRepository;
        this.auditEventService = auditEventService;
        this.cookieName = cookieName;
        this.cookieSecure = cookieSecure;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken csrfToken) {
        return new CsrfResponse(csrfToken.getToken());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (BadCredentialsException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (AuthenticationException exception) {
            throw new AuthenticationServiceException("SalesNav360 authentication could not be completed.", exception);
        }

        SalesNavUserDetails user = (SalesNavUserDetails) authentication.getPrincipal();
        auditEventService.record(user.getUsername(), "LOGIN_SUCCESS", "USER", user.getId(),
                "SalesNav360 user signed in.");
        csrfTokenRepository.saveToken(null, servletRequest, servletResponse);
        ResponseCookie cookie = jwtCookie(jwtService.createToken(user), jwtService.getExpirationSeconds());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(toResponse(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, jwtCookie("", 0).toString())
                .build();
    }

    @GetMapping("/me")
    public AuthResponse currentUser(@AuthenticationPrincipal SalesNavUserDetails user) {
        return toResponse(user);
    }

    private ResponseCookie jwtCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(cookieName, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }

    private AuthResponse toResponse(SalesNavUserDetails user) {
        Set<String> roles = user.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
                .collect(Collectors.toUnmodifiableSet());
        return new AuthResponse(user.getId(), user.getUsername(), user.getEmail(), roles);
    }
}

