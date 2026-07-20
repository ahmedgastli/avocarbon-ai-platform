package com.avocarbon.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.avocarbon.platform.module.identity.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
