package com.salesnav360.security;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalJwtSecretStoreTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void createsAndReusesAStableStrongSecret() {
        Path secretFile = temporaryDirectory.resolve("config").resolve("jwt-secret");

        String firstSecret = LocalJwtSecretStore.loadOrCreate(secretFile);
        String secondSecret = LocalJwtSecretStore.loadOrCreate(secretFile);

        assertEquals(firstSecret, secondSecret);
        assertTrue(firstSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length >= 32);
    }

    @Test
    void createsLocalSecretWhenNoJwtEnvironmentValueIsConfigured() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("localhost");
        Path secretFile = temporaryDirectory.resolve("local").resolve("jwt-secret");

        String secret = SalesNavJwtService.resolveJwtSecret("", environment, secretFile);

        assertEquals(secret, SalesNavJwtService.resolveJwtSecret("", environment, secretFile));
        assertTrue(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length >= 32);
    }
}
