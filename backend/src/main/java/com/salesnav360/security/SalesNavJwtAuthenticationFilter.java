package com.salesnav360.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SalesNavJwtAuthenticationFilter extends OncePerRequestFilter {

    private final SalesNavJwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final String cookieName;

    public SalesNavJwtAuthenticationFilter(
            SalesNavJwtService jwtService,
            UserDetailsService userDetailsService,
            @Value("${salesnav.auth.cookie-name}") String cookieName) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.cookieName = cookieName;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String token = resolveCookieToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            String username = jwtService.usernameFromValidToken(token);
            if (username != null && !username.isBlank()) {
                authenticateActiveUser(username, request);
            }
        }
        filterChain.doFilter(request, response);
    }

    private void authenticateActiveUser(String username, HttpServletRequest request) {
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException exception) {
            SecurityContextHolder.clearContext();
        }
    }

    private String resolveCookieToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
