package com.salesnav360.crm;

import com.salesnav360.repository.RoleRepository;
import com.salesnav360.repository.UserAccountRepository;
import com.salesnav360.security.SalesNavJwtService;
import com.salesnav360.security.SalesNavUserDetails;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:salesnavcrm;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "salesnav.auth.jwt-secret=test-only-salesnav360-signing-secret-at-least-64-characters-long",
        "salesnav.bootstrap.admin-username=crm-admin",
        "salesnav.bootstrap.admin-email=crm-admin@example.test",
        "salesnav.bootstrap.admin-password=TestAdministratorPassword123!"
})
@AutoConfigureMockMvc
class CrmIntegrationTest {

    private static final String AUTH_URL = "/api/v1/salesnav/auth";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private SalesNavJwtService jwtService;

    private Cookie authCookie;

    @BeforeEach
    void authenticateAsAdmin() {
        var account = userAccountRepository.findByUsernameAndEnabledTrue("crm-admin").orElseThrow();
        authCookie = new Cookie("salesnav360", jwtService.createToken(SalesNavUserDetails.from(account)));
    }

    @Test
    void clientRecordsCanBeCreatedSearchedAndArchived() throws Exception {
        Csrf csrf = csrf();
        MvcResult created = mockMvc.perform(post("/api/v1/salesnav/clients")
                        .cookie(authCookie, csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName":"Integration Client",
                                  "email":"integration.client@example.test",
                                  "phone":"+1-555-0199",
                                  "companyName":"Integration Test Company",
                                  "type":"INVESTOR",
                                  "status":"ACTIVE",
                                  "leadSource":"Referral",
                                  "assignedTo":"crm-admin",
                                  "notes":"created by integration test"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("INVESTOR"))
                .andReturn();

        String id = jsonValue(created.getResponse().getContentAsString(), "id");
        mockMvc.perform(get("/api/v1/salesnav/clients")
                        .cookie(authCookie)
                        .param("query", "integration.client@example.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].fullName").value("Integration Client"));
        mockMvc.perform(get("/api/v1/salesnav/clients/duplicates")
                        .cookie(authCookie)
                        .param("email", "INTEGRATION.CLIENT@example.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(Long.parseLong(id)));

        Csrf archiveCsrf = csrf();
        mockMvc.perform(delete("/api/v1/salesnav/clients/" + id)
                        .cookie(authCookie, archiveCsrf.cookie())
                        .header("X-XSRF-TOKEN", archiveCsrf.token()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/salesnav/clients")
                        .cookie(authCookie)
                        .param("query", "integration.client@example.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void leadStagesAreValidatedAndDashboardUsesPersistedMetrics() throws Exception {
        Csrf invalidCreateCsrf = csrf();
        mockMvc.perform(post("/api/v1/salesnav/leads")
                        .cookie(authCookie, invalidCreateCsrf.cookie())
                        .header("X-XSRF-TOKEN", invalidCreateCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName":"Invalid Stage Lead",
                                  "stage":"WON",
                                  "priority":"HIGH"
                                }
                                """))
                .andExpect(status().isConflict());

        Csrf csrf = csrf();
        MvcResult created = mockMvc.perform(post("/api/v1/salesnav/leads")
                        .cookie(authCookie, csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName":"Pipeline Integration Lead",
                                  "email":"pipeline@example.test",
                                  "expectedValue":"12500.50",
                                  "stage":"NEW",
                                  "priority":"HIGH",
                                  "followUpDate":"2030-01-01"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String id = jsonValue(created.getResponse().getContentAsString(), "id");

        Csrf transitionCsrf = csrf();
        mockMvc.perform(patch("/api/v1/salesnav/leads/" + id + "/stage")
                        .cookie(authCookie, transitionCsrf.cookie())
                        .header("X-XSRF-TOKEN", transitionCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stage\":\"WON\"}"))
                .andExpect(status().isConflict());

        transitionCsrf = csrf();
        mockMvc.perform(patch("/api/v1/salesnav/leads/" + id + "/stage")
                        .cookie(authCookie, transitionCsrf.cookie())
                        .header("X-XSRF-TOKEN", transitionCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stage\":\"CONTACTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("CONTACTED"));

        mockMvc.perform(get("/api/v1/salesnav/dashboard").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLeads").value(1))
                .andExpect(jsonPath("$.openLeads").value(1))
                .andExpect(jsonPath("$.activePipelineValue").value(12500.50))
                .andExpect(jsonPath("$.leadStages.CONTACTED").value(1))
                .andExpect(jsonPath("$.recentActivity[0].action").value("LEAD_STAGE_CHANGED"));
    }

    @Test
    void adminCanManageUsersWithoutAllowingNonAdminsIntoAdminEndpoints() throws Exception {
        if (roleRepository.findByName("READ_ONLY").isEmpty()) {
            Csrf roleCsrf = csrf();
            mockMvc.perform(post("/api/v1/salesnav/admin/roles")
                            .cookie(authCookie, roleCsrf.cookie())
                            .header("X-XSRF-TOKEN", roleCsrf.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"READ_ONLY\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$").value("READ_ONLY"));
        }

        String username = "reader-" + Long.toUnsignedString(System.nanoTime());
        Csrf createCsrf = csrf();
        MvcResult created = mockMvc.perform(post("/api/v1/salesnav/admin/users")
                        .cookie(authCookie, createCsrf.cookie())
                        .header("X-XSRF-TOKEN", createCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"%s",
                                  "email":"%s@example.test",
                                  "password":"long-test-password-2026",
                                  "roles":["READ_ONLY"]
                                }
                                """.formatted(username, username)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.roles[0]").value("READ_ONLY"))
                .andReturn();

        var reader = userAccountRepository.findByUsernameAndEnabledTrue(username).orElseThrow();
        Cookie readerCookie = new Cookie("salesnav360",
                jwtService.createToken(SalesNavUserDetails.from(reader)));
        mockMvc.perform(get("/api/v1/salesnav/admin/users").cookie(readerCookie))
                .andExpect(status().isForbidden());

        String id = jsonValue(created.getResponse().getContentAsString(), "id");
        Csrf updateCsrf = csrf();
        mockMvc.perform(patch("/api/v1/salesnav/admin/users/" + id)
                        .cookie(authCookie, updateCsrf.cookie())
                        .header("X-XSRF-TOKEN", updateCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false,\"roles\":[\"READ_ONLY\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void leadConversionCreatesOneClientAndRejectsDuplicateConversions() throws Exception {
        String email = "converted-" + Long.toUnsignedString(System.nanoTime()) + "@example.test";
        String leadId = createQualifiedLead("Conversion Lead", email);

        Csrf convertCsrf = csrf();
        MvcResult converted = mockMvc.perform(post("/api/v1/salesnav/leads/" + leadId + "/convert")
                        .cookie(authCookie, convertCsrf.cookie())
                        .header("X-XSRF-TOKEN", convertCsrf.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("QUALIFIED"))
                .andReturn();
        String clientId = jsonValue(converted.getResponse().getContentAsString(), "convertedClientId");
        mockMvc.perform(get("/api/v1/salesnav/clients/" + clientId).cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("CLIENT"))
                .andExpect(jsonPath("$.status").value("PROSPECT"));

        Csrf repeatCsrf = csrf();
        mockMvc.perform(post("/api/v1/salesnav/leads/" + leadId + "/convert")
                        .cookie(authCookie, repeatCsrf.cookie())
                        .header("X-XSRF-TOKEN", repeatCsrf.token()))
                .andExpect(status().isConflict());

        String duplicateLeadId = createQualifiedLead("Duplicate Conversion Lead", email);
        Csrf duplicateCsrf = csrf();
        mockMvc.perform(post("/api/v1/salesnav/leads/" + duplicateLeadId + "/convert")
                        .cookie(authCookie, duplicateCsrf.cookie())
                        .header("X-XSRF-TOKEN", duplicateCsrf.token()))
                .andExpect(status().isConflict());
    }

    @Test
    void taskWorkflowValidatesStatusAndFeedsDashboardMetrics() throws Exception {
        String dueDate = java.time.LocalDate.now().plusDays(1).toString();
        Csrf createCsrf = csrf();
        MvcResult created = mockMvc.perform(post("/api/v1/salesnav/tasks")
                        .cookie(authCookie, createCsrf.cookie())
                        .header("X-XSRF-TOKEN", createCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Call client about renewal",
                                  "description":"Discuss available options.",
                                  "dueDate":"%s",
                                  "priority":"HIGH",
                                  "assignedTo":"crm-admin"
                                }
                                """.formatted(dueDate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn();
        String id = jsonValue(created.getResponse().getContentAsString(), "id");
        mockMvc.perform(get("/api/v1/salesnav/dashboard").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeTasks").value(1))
                .andExpect(jsonPath("$.upcomingTasks[0].title").value("Call client about renewal"));

        Csrf statusCsrf = csrf();
        mockMvc.perform(patch("/api/v1/salesnav/tasks/" + id + "/status")
                        .cookie(authCookie, statusCsrf.cookie())
                        .header("X-XSRF-TOKEN", statusCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
        mockMvc.perform(get("/api/v1/salesnav/dashboard").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeTasks").value(0))
                .andExpect(jsonPath("$.completedTasks").value(1));
    }

    @Test
    void policyRequiresActiveClientAndValidDates() throws Exception {
        Csrf clientCsrf = csrf();
        MvcResult client = mockMvc.perform(post("/api/v1/salesnav/clients")
                        .cookie(authCookie, clientCsrf.cookie())
                        .header("X-XSRF-TOKEN", clientCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName":"Policy Integration Client",
                                  "type":"CLIENT",
                                  "status":"ACTIVE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String clientId = jsonValue(client.getResponse().getContentAsString(), "id");
        String policyRequest = """
                {
                  "policyNumber":"POL-%s",
                  "policyType":"Life",
                  "clientId":%s,
                  "provider":"Example Insurer",
                  "premiumAmount":"1200.50",
                  "coverageAmount":"100000.00",
                  "startDate":"2030-01-01",
                  "expiryDate":"2031-01-01",
                  "renewalDate":"2030-12-01",
                  "status":"ACTIVE",
                  "assignedTo":"crm-admin"
                }
                """.formatted(Long.toUnsignedString(System.nanoTime()), clientId);
        Csrf policyCsrf = csrf();
        MvcResult policy = mockMvc.perform(post("/api/v1/salesnav/policies")
                        .cookie(authCookie, policyCsrf.cookie())
                        .header("X-XSRF-TOKEN", policyCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(policyRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientId").value(Long.parseLong(clientId)))
                .andExpect(jsonPath("$.premiumAmount").value(1200.50))
                .andReturn();
        String policyId = jsonValue(policy.getResponse().getContentAsString(), "id");

        mockMvc.perform(get("/api/v1/salesnav/policies")
                        .cookie(authCookie)
                        .param("query", "Policy Integration Client"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        Csrf invalidCsrf = csrf();
        mockMvc.perform(put("/api/v1/salesnav/policies/" + policyId)
                        .cookie(authCookie, invalidCsrf.cookie())
                        .header("X-XSRF-TOKEN", invalidCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(policyRequest.replace("\"startDate\":\"2030-01-01\"", "\"startDate\":\"2032-01-01\"")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void quotationTotalsUseDecimalRoundingAndAcceptanceIsNotAClosedSale() throws Exception {
        Csrf clientCsrf = csrf();
        MvcResult client = mockMvc.perform(post("/api/v1/salesnav/clients")
                        .cookie(authCookie, clientCsrf.cookie())
                        .header("X-XSRF-TOKEN", clientCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName":"Quotation Integration Client",
                                  "type":"CLIENT",
                                  "status":"ACTIVE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String clientId = jsonValue(client.getResponse().getContentAsString(), "id");
        String request = """
                {
                  "clientId":%s,
                  "validUntil":"2030-12-31",
                  "discountPercent":"10.00",
                  "taxPercent":"18.00",
                  "lineItems":[
                    {"description":"Annual service","quantity":"2","unitPrice":"10.10"}
                  ]
                }
                """.formatted(clientId);
        Csrf quoteCsrf = csrf();
        MvcResult quote = mockMvc.perform(post("/api/v1/salesnav/quotations")
                        .cookie(authCookie, quoteCsrf.cookie())
                        .header("X-XSRF-TOKEN", quoteCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.subtotal").value(20.20))
                .andExpect(jsonPath("$.discount").value(2.02))
                .andExpect(jsonPath("$.tax").value(3.27))
                .andExpect(jsonPath("$.grandTotal").value(21.45))
                .andExpect(jsonPath("$.lineItems[0].lineTotal").value(20.20))
                .andReturn();
        String quoteId = jsonValue(quote.getResponse().getContentAsString(), "id");

        MvcResult pdf = mockMvc.perform(get("/api/v1/salesnav/quotations/" + quoteId + "/pdf")
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn();
        assertEquals("%PDF-", new String(pdf.getResponse().getContentAsByteArray(), 0, 5, StandardCharsets.US_ASCII));

        Csrf sentCsrf = csrf();
        mockMvc.perform(patch("/api/v1/salesnav/quotations/" + quoteId + "/status")
                        .cookie(authCookie, sentCsrf.cookie())
                        .header("X-XSRF-TOKEN", sentCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SENT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"));
        Csrf acceptedCsrf = csrf();
        mockMvc.perform(patch("/api/v1/salesnav/quotations/" + quoteId + "/status")
                        .cookie(authCookie, acceptedCsrf.cookie())
                        .header("X-XSRF-TOKEN", acceptedCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        Csrf updateCsrf = csrf();
        mockMvc.perform(put("/api/v1/salesnav/quotations/" + quoteId)
                        .cookie(authCookie, updateCsrf.cookie())
                        .header("X-XSRF-TOKEN", updateCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict());
    }

    @Test
    void crmEndpointsRequireSalesNavAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/salesnav/dashboard"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/salesnav/clients"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsAssignmentsToUnknownSalesNavUsers() throws Exception {
        Csrf csrf = csrf();
        mockMvc.perform(post("/api/v1/salesnav/clients")
                        .cookie(authCookie, csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName":"Invalid Assignment",
                                  "type":"CLIENT",
                                  "status":"ACTIVE",
                                  "assignedTo":"not-an-active-user"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    private Csrf csrf() throws Exception {
        MvcResult result = mockMvc.perform(get(AUTH_URL + "/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        Matcher matcher = Pattern.compile("\"token\":\"([^\"]+)\"")
                .matcher(result.getResponse().getContentAsString());
        if (!matcher.find()) {
            throw new AssertionError("CSRF endpoint response did not contain a token.");
        }
        return new Csrf(matcher.group(1), result.getResponse().getCookie("XSRF-TOKEN"));
    }

    private String createQualifiedLead(String fullName, String email) throws Exception {
        Csrf createCsrf = csrf();
        MvcResult created = mockMvc.perform(post("/api/v1/salesnav/leads")
                        .cookie(authCookie, createCsrf.cookie())
                        .header("X-XSRF-TOKEN", createCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName":"%s",
                                  "email":"%s",
                                  "stage":"NEW",
                                  "priority":"MEDIUM"
                                }
                                """.formatted(fullName, email)))
                .andExpect(status().isCreated())
                .andReturn();
        String leadId = jsonValue(created.getResponse().getContentAsString(), "id");
        moveLeadToStage(leadId, "CONTACTED");
        moveLeadToStage(leadId, "QUALIFIED");
        return leadId;
    }

    private void moveLeadToStage(String id, String stage) throws Exception {
        Csrf csrf = csrf();
        mockMvc.perform(patch("/api/v1/salesnav/leads/" + id + "/stage")
                        .cookie(authCookie, csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stage\":\"" + stage + "\"}"))
                .andExpect(status().isOk());
    }

    private static String jsonValue(String json, String property) {
        Matcher matcher = Pattern.compile("\"" + property + "\":(\\d+)").matcher(json);
        if (!matcher.find()) {
            throw new AssertionError("Response did not contain " + property);
        }
        return matcher.group(1);
    }

    private record Csrf(String token, Cookie cookie) {
    }
}
