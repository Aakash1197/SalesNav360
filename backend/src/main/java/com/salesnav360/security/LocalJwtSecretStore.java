package com.salesnav360.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.Base64;

final class LocalJwtSecretStore {

    private LocalJwtSecretStore() {
    }

    static String loadOrCreate(Path secretFile) {
        try {
            Files.createDirectories(secretFile.getParent());
            byte[] secretBytes = new byte[64];
            new SecureRandom().nextBytes(secretBytes);
            String generatedSecret = Base64.getEncoder().encodeToString(secretBytes);
            try {
                Files.writeString(
                        secretFile,
                        generatedSecret,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW,
                        StandardOpenOption.WRITE);
            } catch (FileAlreadyExistsException ignored) {
                // Another application startup created the persistent key first.
            }
            String savedSecret = Files.readString(secretFile, StandardCharsets.UTF_8).trim();
            if (savedSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
                throw new IllegalStateException("The saved local JWT key is too short: " + secretFile);
            }
            return savedSecret;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read or create the local JWT key at " + secretFile, exception);
        }
    }
}
