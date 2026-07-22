package com.avocarbon.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.avocarbon.platform.module.identity.*;
import com.avocarbon.platform.module.project.*;
import com.avocarbon.platform.module.integration.*;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Sprint1IntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    private String token;
    private User testUser;

    @BeforeEach
    void setUp() throws Exception {
        dataSourceRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        // Seed a test PRODUCTION_MANAGER user
        testUser = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@avocarbon.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.PRODUCTION_MANAGER)
                .enabled(true)
                .assignedSites(java.util.List.of("luxembourg"))
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
    void testSprint1UserProjectApiFlow() throws Exception {
        // 1. Create a Project with a start date
        Instant projectStartDate = Instant.now();
        ProjectRequest projectRequest = ProjectRequest.builder()
                .name("Line 1 Optimization")
                .description("Project to track and reduce scrap rate in Line 1 production.")
                .ownerId(testUser.getId())
                .startDate(projectStartDate)
                .siteId("luxembourg")
                .build();

        MvcResult projectResult = mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name").value("Line 1 Optimization"))
                .andExpect(jsonPath("$.startDate", notNullValue()))
                .andReturn();

        String projectResponseContent = projectResult.getResponse().getContentAsString();
        Long projectId = objectMapper.readTree(projectResponseContent).get("id").asLong();

        // 2. Add a DataSource (API) to the project
        DataSourceRequest dataSourceRequest = DataSourceRequest.builder()
                .name("Line 1 Production API")
                .url("http://internal-api.avocarbon.com/prod/line1")
                .token("super-secret-token")
                .type(DataSourceType.PRODUCTION)
                .syncFrequency(SyncFrequency.DAILY)
                .siteId("luxembourg")
                .build();

        MvcResult dsResult = mockMvc.perform(post("/api/projects/" + projectId + "/datasources")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dataSourceRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name").value("Line 1 Production API"))
                .andExpect(jsonPath("$.type").value("PRODUCTION"))
                .andExpect(jsonPath("$.syncFrequency").value("DAILY"))
                .andExpect(jsonPath("$.projectId").value(projectId))
                .andReturn();

        String dsResponseContent = dsResult.getResponse().getContentAsString();
        Long dataSourceId = objectMapper.readTree(dsResponseContent).get("id").asLong();

        // 3. List data sources for the project
        mockMvc.perform(get("/api/projects/" + projectId + "/datasources")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Line 1 Production API"));

        // 4. Update the data source
        DataSourceRequest updateRequest = DataSourceRequest.builder()
                .name("Line 1 Production API (V2)")
                .url("http://internal-api.avocarbon.com/prod/line1/v2")
                .token("new-token")
                .type(DataSourceType.PRODUCTION)
                .syncFrequency(SyncFrequency.WEEKLY)
                .siteId("luxembourg")
                .build();

        mockMvc.perform(put("/api/datasources/" + dataSourceId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Line 1 Production API (V2)"))
                .andExpect(jsonPath("$.url").value("http://internal-api.avocarbon.com/prod/line1/v2"))
                .andExpect(jsonPath("$.syncFrequency").value("WEEKLY"));

        // 5. Delete the data source
        mockMvc.perform(delete("/api/datasources/" + dataSourceId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Verify it is gone
        mockMvc.perform(get("/api/projects/" + projectId + "/datasources")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
