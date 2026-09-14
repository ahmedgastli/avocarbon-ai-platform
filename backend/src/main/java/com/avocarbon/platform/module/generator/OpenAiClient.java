package com.avocarbon.platform.module.generator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Direct Azure OpenAI client for Angular code synthesis.
 *
 * Used as a secondary generation path when an MCP server is not
 * configured, or can be invoked directly for prompt-based generation.
 *
 * When the API key is "mock-openai-key" (default), the client returns
 * an empty map and McpClient's built-in mock handles generation instead.
 *
 * Configuration:
 *   app.openai.api-key=<key>
 *   app.openai.endpoint=https://<resource>.openai.azure.com
 *   app.openai.deployment=gpt-4o
 */
@Component
@Slf4j
public class OpenAiClient {

    private final String apiKey;
    private final String endpoint;
    private final String deployment;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OpenAiClient(
            @Value("${app.openai.api-key:mock-openai-key}")    String apiKey,
            @Value("${app.openai.endpoint:https://mock.azure.com}") String endpoint,
            @Value("${app.openai.deployment:gpt-4o}")          String deployment,
            ObjectMapper objectMapper) {
        this.apiKey     = apiKey;
        this.endpoint   = endpoint;
        this.deployment = deployment;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().build();
    }

    /**
     * Call Azure OpenAI to synthesise Angular code directly from the prompt.
     *
     * Returns an empty map when running in mock mode (api-key = mock-openai-key)
     * so that the caller (McpClient fallback or GenerationJobExecutor) can handle it.
     *
     * @param prompt full generation prompt from {@link PromptBuilder}
     * @param spec   parsed spec for context
     * @param type   generation type
     * @return map of filePath → fileContent (empty in mock mode)
     */
    public Map<String, String> generateAngularCode(String prompt,
                                                    ParsedOpenApiSpec spec,
                                                    GenerationType type) {
        boolean isMock = "mock-openai-key".equals(apiKey) || endpoint.contains("mock");
        if (isMock) {
            log.info("OpenAiClient running in mock mode — returning empty map (McpClient mock will handle generation)");
            return new LinkedHashMap<>();
        }

        log.info("Calling Azure OpenAI at '{}' (deployment: {})", endpoint, deployment);
        try {
            String url = endpoint + "/openai/deployments/" + deployment + "/chat/completions?api-version=2024-02-01";

            Map<String, Object> message  = Map.of("role", "user", "content", prompt);
            Map<String, Object> body     = Map.of(
                    "messages",    List.of(message),
                    "max_tokens",  16000,
                    "temperature", 0.2
            );

            String responseJson = restClient.post()
                    .uri(url)
                    .header("api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return extractFilesFromResponse(responseJson);

        } catch (Exception e) {
            log.warn("Azure OpenAI call failed: {}", e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    // -------------------------------------------------------------------------
    // Response parsing
    // -------------------------------------------------------------------------

    private Map<String, String> extractFilesFromResponse(String json) {
        try {
            Map<String, Object> response = objectMapper.readValue(json, new TypeReference<>() {});
            List<?> choices = (List<?>) response.get("choices");
            if (choices == null || choices.isEmpty()) return new LinkedHashMap<>();

            Map<?, ?> choice  = (Map<?, ?>) choices.get(0);
            Map<?, ?> message = (Map<?, ?>) choice.get("message");
            if (message == null) return new LinkedHashMap<>();

            String content = String.valueOf(message.get("content"));
            // Extract JSON block from markdown code fence if present
            int start = content.indexOf('{');
            int end   = content.lastIndexOf('}');
            if (start >= 0 && end > start) {
                String jsonPart = content.substring(start, end + 1);
                Map<String, String> files = objectMapper.readValue(jsonPart, new TypeReference<>() {});
                log.info("OpenAI returned {} files", files.size());
                return files;
            }
        } catch (Exception e) {
            log.warn("Failed to parse OpenAI response: {}", e.getMessage());
        }
        return new LinkedHashMap<>();
    }
}
