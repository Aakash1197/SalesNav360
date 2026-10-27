package com.salesnav360.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

@Service
public class SalesNavJwtService {

    private final SecretKey signingKey;
    private final long expirationSeconds;

    @Autowired
    public SalesNavJwtService(
            @Value("${salesnav.auth.jwt-secret}") String jwtSecret,
            @Value("${salesnav.auth.jwt-expiration-seconds}") long expirationSeconds,
            Environment environment) {
        this(resolveJwtSecret(
                jwtSecret,
                environment,
                Path.of(System.getProperty("user.home"), ".salesnav360", "jwt-secret")),
                expirationSeconds);
    }

    public SalesNavJwtService(String jwtSecret, long expirationSeconds) {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = expirationSeconds;
    }

    static String resolveJwtSecret(String configuredSecret, Environment environment, Path localSecretFile) {
        if (!configuredSecret.isBlank()) {
            return configuredSecret;
        }
        if (environment.acceptsProfiles(Profiles.of("localhost"))) {
            return LocalJwtSecretStore.loadOrCreate(localSecretFile);
        }
        throw new IllegalStateException(
                "SALESNAV_JWT_SECRET must be set to a strong key of at least 32 bytes outside localhost.");
    }

    public String createToken(SalesNavUserDetails user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(user.getUsername())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(signingKey)
                .compact();
    }

    public String usernameFromValidToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (JwtException | IllegalArgumentException exception) {
            return null;
        }
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}
