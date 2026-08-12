package com.avocarbon.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.avocarbon.platform.module.identity.LoginRequest;
import com.avocarbon.platform.module.identity.Role;
import com.avocarbon.platform.module.identity.User;
import com.avocarbon.platform.module.identity.UserRepository;
import com.avocarbon.platform.module.analytics.KpiAggregationRepository;
import com.avocarbon.platform.module.generator.GeneratedReportRepository;
import com.avocarbon.platform.module.integration.DataSourceRepository;
import com.avocarbon.platform.module.integration.IntegrationSyncLogRepository;
import com.avocarbon.platform.module.integration.ProductionMetricRepository;
import com.avocarbon.platform.module.integration.QualityMetricRepository;
import com.avocarbon.platform.module.integration.CustomerMetricRepository;
import com.avocarbon.platform.module.integration.MaintenanceMetricRepository;
import com.avocarbon.platform.module.project.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityVerificationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    // All dependent repositories needed to delete in proper FK order
    @Autowired private GeneratedReportRepository generatedReportRepository;
    @Autowired private KpiAggregationRepository kpiAggregationRepository;
    @Autowired private IntegrationSyncLogRepository syncLogRepository;
    @Autowired private ProductionMetricRepository productionMetricRepository;
    @Autowired private QualityMetricRepository qualityMetricRepository;
    @Autowired private CustomerMetricRepository customerMetricRepository;
    @Autowired private MaintenanceMetricRepository maintenanceMetricRepository;
    @Autowired private DataSourceRepository dataSourceRepository;
    @Autowired private ProjectRepository projectRepository;

    @BeforeEach
    void setUp() {
        // Clean in FK dependency order (children before parents)
        generatedReportRepository.deleteAll();
        kpiAggregationRepository.deleteAll();
        syncLogRepository.deleteAll();
        productionMetricRepository.deleteAll();
        qualityMetricRepository.deleteAll();
        customerMetricRepository.deleteAll();
        maintenanceMetricRepository.deleteAll();
        dataSourceRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        // Seed a self-contained admin user for this test
        userRepository.save(User.builder()
                .firstName("Admin")
                .lastName("AVOCarbon")
                .email("admin@avocarbon.com")
                .password(passwordEncoder.encode("admin12345"))
                .role(Role.ADMIN)
                .enabled(true)
                .build());
    }

    @Test
    void testSecurityAuthenticationFlow() throws Exception {
        // 1. Verify that GET /api/users is blocked (401 Unauthorized)
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());

        // 2. Authenticate using seeded Admin credentials
        LoginRequest loginRequest = LoginRequest.builder()
                .email("admin@avocarbon.com")
                .password("admin12345")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.email").value("admin@avocarbon.com"))
                .andReturn();

        // 3. Extract the JWT token
        String responseContent = result.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseContent).get("token").asText();

        // 4. Access GET /api/users using the Bearer token (should succeed with 200 OK)
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // 5. Verify that Swagger paths are allowed without authentication (200 OK)
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }
}
