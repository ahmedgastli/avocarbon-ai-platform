package com.avocarbon.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.avocarbon.platform.module.identity.*;
import com.avocarbon.platform.module.project.*;
import com.avocarbon.platform.module.integration.*;
import com.avocarbon.platform.module.analytics.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Sprint2IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @Autowired
    private ProductionMetricRepository productionMetricRepository;

    @Autowired
    private QualityMetricRepository qualityMetricRepository;

    @Autowired
    private CustomerMetricRepository customerMetricRepository;

    @Autowired
    private MaintenanceMetricRepository maintenanceMetricRepository;

    @Autowired
    private KpiAggregationRepository kpiAggregationRepository;

    @Autowired
    private IntegrationSyncLogRepository syncLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String token;
    private User testUser;

    @BeforeEach
    void setUp() throws Exception {
        syncLogRepository.deleteAll();
        kpiAggregationRepository.deleteAll();
        productionMetricRepository.deleteAll();
        qualityMetricRepository.deleteAll();
        customerMetricRepository.deleteAll();
        maintenanceMetricRepository.deleteAll();
        dataSourceRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        // Seed user
        testUser = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@avocarbon.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.ADMIN)
                .enabled(true)
                .build();
        userRepository.save(testUser);

        // Authenticate
        LoginRequest loginRequest = LoginRequest.builder()
                .email("john.doe@avocarbon.com")
                .password("password123")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        token = objectMapper.readTree(responseContent).get("token").asText();
    }

    @Test
    void testDataSyncAndKpiCalculationFlow() throws Exception {
        // 1. Create a Project
        ProjectRequest projectRequest = ProjectRequest.builder()
                .name("Line 1 Optimization")
                .description("Project scrap rate tracking.")
                .ownerId(testUser.getId())
                .startDate(Instant.now())
                .build();

        MvcResult projectResult = mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long projectId = objectMapper.readTree(projectResult.getResponse().getContentAsString()).get("id").asLong();

        // 2. Add a Production Data Source
        DataSourceRequest prodRequest = DataSourceRequest.builder()
                .name("Production Line 1 API")
                .url("http://mock-api.avocarbon.com/prod")
                .token("mock-token-1")
                .type(DataSourceType.PRODUCTION)
                .syncFrequency(SyncFrequency.DAILY)
                .build();

        MvcResult prodDsResult = mockMvc.perform(post("/api/projects/" + projectId + "/datasources")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prodRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long prodDsId = objectMapper.readTree(prodDsResult.getResponse().getContentAsString()).get("id").asLong();

        // 3. Add a Quality Data Source
        DataSourceRequest qualRequest = DataSourceRequest.builder()
                .name("Quality Line 1 API")
                .url("http://mock-api.avocarbon.com/quality")
                .token("mock-token-2")
                .type(DataSourceType.QUALITY)
                .syncFrequency(SyncFrequency.DAILY)
                .build();

        MvcResult qualDsResult = mockMvc.perform(post("/api/projects/" + projectId + "/datasources")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(qualRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long qualDsId = objectMapper.readTree(qualDsResult.getResponse().getContentAsString()).get("id").asLong();

        // 4. Trigger manual synchronization for both sources
        mockMvc.perform(post("/api/datasources/" + prodDsId + "/sync")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("31")); // 30 days history + 1 current day

        mockMvc.perform(post("/api/datasources/" + qualDsId + "/sync")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("31"));

        // 5. Verify database metrics exist
        List<ProductionMetric> pMetrics = productionMetricRepository.findByDataSourceId(prodDsId);
        List<QualityMetric> qMetrics = qualityMetricRepository.findByDataSourceId(qualDsId);
        assertEquals(31, pMetrics.size());
        assertEquals(31, qMetrics.size());

        // 6. Verify sync logs exist
        List<IntegrationSyncLog> logs = syncLogRepository.findByDataSourceId(prodDsId);
        assertEquals(1, logs.size());
        assertEquals("SUCCESS", logs.get(0).getStatus());

        // 7. Verify KPI Summary
        MvcResult summaryResult = mockMvc.perform(get("/api/analytics/projects/" + projectId + "/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(projectId))
                .andExpect(jsonPath("$.oee").isNumber())
                .andExpect(jsonPath("$.scrapRate").isNumber())
                .andReturn();

        double oee = objectMapper.readTree(summaryResult.getResponse().getContentAsString()).get("oee").asDouble();
        assertTrue(oee > 0.0 && oee <= 1.0);

        // 8. Verify KPI History
        mockMvc.perform(get("/api/analytics/projects/" + projectId + "/history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(31)))
                .andExpect(jsonPath("$[0].aggregationPeriod").value("DAILY"));
    }
}
