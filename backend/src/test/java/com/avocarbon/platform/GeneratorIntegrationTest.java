package com.avocarbon.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.avocarbon.platform.module.identity.*;
import com.avocarbon.platform.module.project.*;
import com.avocarbon.platform.module.analytics.*;
import com.avocarbon.platform.module.generator.*;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GeneratorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private KpiAggregationRepository kpiAggregationRepository;

    @Autowired
    private GeneratedReportRepository generatedReportRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String managerToken;
    private String unauthorizedToken;
    private User managerUser;
    private User otherUser;
    private Project project;

    @BeforeEach
    void setUp() throws Exception {
        generatedReportRepository.deleteAll();
        kpiAggregationRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Seed manager user with access to site "luxembourg"
        managerUser = User.builder()
                .firstName("Alice")
                .lastName("Manager")
                .email("alice@avocarbon.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.PRODUCTION_MANAGER)
                .enabled(true)
                .assignedSites(List.of("luxembourg"))
                .build();
        userRepository.save(managerUser);

        // 2. Seed another user with access to site "brussels"
        otherUser = User.builder()
                .firstName("Bob")
                .lastName("User")
                .email("bob@avocarbon.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.QUALITY_MANAGER)
                .enabled(true)
                .assignedSites(List.of("brussels"))
                .build();
        userRepository.save(otherUser);

        // 3. Authenticate manager
        LoginRequest managerLogin = LoginRequest.builder()
                .email("alice@avocarbon.com")
                .password("password123")
                .build();

        MvcResult result1 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(managerLogin)))
                .andExpect(status().isOk())
                .andReturn();
        managerToken = objectMapper.readTree(result1.getResponse().getContentAsString()).get("token").asText();

        // 4. Authenticate unauthorized user
        LoginRequest otherLogin = LoginRequest.builder()
                .email("bob@avocarbon.com")
                .password("password123")
                .build();

        MvcResult result2 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherLogin)))
                .andExpect(status().isOk())
                .andReturn();
        unauthorizedToken = objectMapper.readTree(result2.getResponse().getContentAsString()).get("token").asText();

        // 5. Create a project at "luxembourg"
        project = Project.builder()
                .name("Line 1 Optimization")
                .description("Reducing scrap rate")
                .status(ProjectStatus.CREATED)
                .startDate(Instant.now())
                .owner(managerUser)
                .siteId("luxembourg")
                .build();
        projectRepository.save(project);

        // 6. Seed KPI aggregation details
        KpiAggregation kpi = KpiAggregation.builder()
                .project(project)
                .calculationTimestamp(Instant.now())
                .aggregationPeriod("OVERALL")
                .availability(0.92)
                .performance(0.88)
                .quality(0.98)
                .oee(0.79)
                .scrapRate(0.02)
                .customerSatisfaction(94.0)
                .maintenanceDowntime(120.0)
                .build();
        kpiAggregationRepository.save(kpi);
    }

    @Test
    void testSecurityConstraints() throws Exception {
        GeneratedReportRequest reportRequest = GeneratedReportRequest.builder()
                .title("Monthly OEE Evaluation")
                .type(ReportType.OEE_ANALYSIS)
                .promptSummary("Analyze downtime reasons")
                .build();

        // 1. Unauthenticated requests should return 401 Unauthorized
        mockMvc.perform(post("/api/projects/" + project.getId() + "/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportRequest)))
                .andExpect(status().isUnauthorized());

        // 2. Bob lacks access to site "luxembourg" -> should return 403 Forbidden
        mockMvc.perform(post("/api/projects/" + project.getId() + "/reports")
                        .header("Authorization", "Bearer " + unauthorizedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testReportCRUDAndAiSimulation() throws Exception {
        GeneratedReportRequest reportRequest = GeneratedReportRequest.builder()
                .title("Scrap Optimization Report")
                .type(ReportType.SCRAP_RATE_ANOMALY)
                .promptSummary("Examine quality bottlenecks")
                .build();

        // 1. Generate Report
        MvcResult generateResult = mockMvc.perform(post("/api/projects/" + project.getId() + "/reports")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.projectId").value(project.getId()))
                .andExpect(jsonPath("$.title").value("Scrap Optimization Report"))
                .andExpect(jsonPath("$.type").value("SCRAP_RATE_ANOMALY"))
                .andExpect(jsonPath("$.content", containsString("Scrap Rate & Defects Analysis")))
                .andExpect(jsonPath("$.content", containsString("Quality:")))
                .andExpect(jsonPath("$.generatedByName").value("Alice Manager"))
                .andReturn();

        Long reportId = objectMapper.readTree(generateResult.getResponse().getContentAsString()).get("id").asLong();

        // 2. Fetch Reports for Project
        mockMvc.perform(get("/api/projects/" + project.getId() + "/reports")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(reportId))
                .andExpect(jsonPath("$[0].title").value("Scrap Optimization Report"));

        // 3. Fetch Report Details by ID
        mockMvc.perform(get("/api/reports/" + reportId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reportId))
                .andExpect(jsonPath("$.content", containsString("AI Analysis")));

        // 4. Delete Report
        mockMvc.perform(delete("/api/reports/" + reportId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isNoContent());

        // 5. Verify Deleted
        mockMvc.perform(get("/api/reports/" + reportId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isNotFound());
    }
}
