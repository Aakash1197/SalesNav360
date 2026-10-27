package com.salesnav360.auth;

import com.salesnav360.entity.Role;
import com.salesnav360.entity.UserAccount;
import com.salesnav360.repository.RoleRepository;
import com.salesnav360.repository.UserAccountRepository;
import com.salesnav360.security.SalesNavJwtService;
import com.salesnav360.security.SalesNavUserDetails;
import jakarta.servlet.http.Cookie;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:salesnav;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "salesnav.auth.jwt-secret=test-only-salesnav360-signing-secret-at-least-64-characters-long",
        "salesnav.bootstrap.admin-username=bootstrap-admin",
        "salesnav.bootstrap.admin-email=bootstrap-admin@example.test",
        "salesnav.bootstrap.admin-password=TestAdministratorPassword123!"
})
@AutoConfigureMockMvc
class AuthIntegrationTest {

    private static final String AUTH_URL = "/api/v1/salesnav/auth";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private SalesNavJwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void requiresAuthenticationForCurrentUserAndAdminAccess() throws Exception {
        mockMvc.perform(get(AUTH_URL + "/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/salesnav/admin/access"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deniesNonAdminRoleFromAdminEndpoint() throws Exception {
        Role salesRepresentativeRole = roleRepository.save(new Role("SALES_REPRESENTATIVE"));
        UserAccount salesRepresentative = userAccountRepository.save(new UserAccount(
                "sales-representative",
                "sales-representative@example.test",
                passwordEncoder.encode("TestSalesRepresentativePassword123!"),
                Set.of(salesRepresentativeRole)));
        String token = jwtService.createToken(SalesNavUserDetails.from(salesRepresentative));

        mockMvc.perform(get("/api/v1/salesnav/admin/access")
                        .cookie(new Cookie("salesnav360", token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void logsInBootstrappedAdminAndUsesSignedHttpOnlyCookie() throws Exception {
        MvcResult csrfResult = mockMvc.perform(get(AUTH_URL + "/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        Matcher tokenMatcher = Pattern.compile("\"token\":\"([^\"]+)\"")
                .matcher(csrfResult.getResponse().getContentAsString());
        assertTrue(tokenMatcher.find());
        String csrfToken = tokenMatcher.group(1);
        Cookie csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(csrfCookie);

        MvcResult loginResult = mockMvc.perform(post(AUTH_URL + "/login")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"bootstrap-admin","password":"TestAdministratorPassword123!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("bootstrap-admin"))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
                .andReturn();

        Cookie sessionCookie = loginResult.getResponse().getCookie("salesnav360");
        assertNotNull(sessionCookie);
        assertTrue(sessionCookie.isHttpOnly());

        mockMvc.perform(get(AUTH_URL + "/me").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("bootstrap-admin@example.test"));
        mockMvc.perform(get("/api/v1/salesnav/admin/access").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"));
    }

    @Test
    void rejectsStateChangingRequestsWithoutCsrfToken() throws Exception {
        mockMvc.perform(post(AUTH_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"bootstrap-admin","password":"wrong"}
                                """))
                .andExpect(status().isForbidden());
    }
}

