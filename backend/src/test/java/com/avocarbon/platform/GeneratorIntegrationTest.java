package com.avocarbon.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.avocarbon.platform.module.identity.*;
import com.avocarbon.platform.module.project.*;
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
    private GenerationJobRepository generationJobRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String managerToken;
    private String unauthorizedToken;
    private User managerUser;
    private User otherUser;
    private Project project;

    @BeforeEach
    void setUp() throws Exception {
        generationJobRepository.deleteAll();
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
    }

    @Test
    void testSecurityConstraints() throws Exception {
        GenerationJobRequest request = GenerationJobRequest.builder()
                .name("Frontend Generation Job")
                .type(GenerationType.ANGULAR_FULL)
                .openApiContent("openapi: 3.0.0\ninfo:\n  title: Sample\n  version: 1.0.0\npaths:\n  /test:\n    get:\n      responses:\n        '200':\n          description: OK")
                .build();

        // 1. Unauthenticated requests should return 401 Unauthorized
        mockMvc.perform(post("/api/projects/" + project.getId() + "/generations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        // 2. Bob lacks access to site "luxembourg" -> should return 403 Forbidden
        mockMvc.perform(post("/api/projects/" + project.getId() + "/generations")
                        .header("Authorization", "Bearer " + unauthorizedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGenerationJobLifecycle() throws Exception {
        String validOpenApi = "{\n" +
                "  \"openapi\": \"3.0.0\",\n" +
                "  \"info\": { \"title\": \"Mock App\", \"version\": \"1.0\" },\n" +
                "  \"paths\": {\n" +
                "    \"/users\": {\n" +
                "      \"get\": {\n" +
                "        \"tags\": [\"Users\"],\n" +
                "        \"responses\": { \"200\": { \"description\": \"OK\" } }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";

        GenerationJobRequest request = GenerationJobRequest.builder()
                .name("Angular Test App")
                .type(GenerationType.ANGULAR_FULL)
                .openApiFormat("JSON")
                .openApiContent(validOpenApi)
                .build();

        // 1. Submit Generation Job
        MvcResult submitResult = mockMvc.perform(post("/api/projects/" + project.getId() + "/generations")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.projectId").value(project.getId()))
                .andExpect(jsonPath("$.name").value("Angular Test App"))
                .andExpect(jsonPath("$.type").value("ANGULAR_FULL"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.triggeredByName").value("Alice Manager"))
                .andReturn();

        Long jobId = objectMapper.readTree(submitResult.getResponse().getContentAsString()).get("id").asLong();

        // 2. Poll until SUCCESS (Async execution runs in background)
        boolean isSuccess = false;
        int retries = 0;
        while (!isSuccess && retries < 20) {
            Thread.sleep(500); // Poll every 500ms
            MvcResult pollResult = mockMvc.perform(get("/api/generations/" + jobId)
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isOk())
                    .andReturn();
            String status = objectMapper.readTree(pollResult.getResponse().getContentAsString()).get("status").asText();
            if ("SUCCESS".equals(status)) {
                isSuccess = true;
            } else if ("FAILED".equals(status)) {
                fail("Job failed unexpectedly: " + pollResult.getResponse().getContentAsString());
            }
            retries++;
        }
        assertTrue(isSuccess, "Job did not complete successfully within 10 seconds");

        // 3. Fetch Jobs for Project
        mockMvc.perform(get("/api/projects/" + project.getId() + "/generations")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(jobId))
                .andExpect(jsonPath("$[0].status").value("SUCCESS"));

        // 4. Download Generated ZIP
        mockMvc.perform(get("/api/generations/" + jobId + "/download")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String disposition = result.getResponse().getHeader("Content-Disposition");
                    assertNotNull(disposition);
                    assertTrue(disposition.contains("attachment"));
                    assertTrue(disposition.contains("angular-test-app-angular.zip"));
                    assertTrue(result.getResponse().getContentAsByteArray().length > 0);
                });

        // 5. Delete Job
        mockMvc.perform(delete("/api/generations/" + jobId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isNoContent());

        // 6. Verify Deleted
        mockMvc.perform(get("/api/generations/" + jobId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isNotFound());
    }
}
