package com.salesnav360.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SalesNavJwtServiceTest {

    private static final String SECRET = "test-only-salesnav360-signing-secret-at-least-64-characters-long";

    @Test
    void signsAndValidatesSalesNavToken() {
        SalesNavJwtService service = new SalesNavJwtService(SECRET, 3600);
        SalesNavUserDetails user = mock(SalesNavUserDetails.class);
        when(user.getUsername()).thenReturn("sales.admin");

        String token = service.createToken(user);

        assertEquals("sales.admin", service.usernameFromValidToken(token));
    }

    @Test
    void rejectsTokenSignedByAnotherApplication() {
        SalesNavJwtService salesNavService = new SalesNavJwtService(SECRET, 3600);
        SalesNavJwtService otherService = new SalesNavJwtService(
                "another-application-signing-secret-at-least-64-characters-long", 3600);
        SalesNavUserDetails user = mock(SalesNavUserDetails.class);
        when(user.getUsername()).thenReturn("sales.admin");

        assertNull(salesNavService.usernameFromValidToken(otherService.createToken(user)));
    }
}

