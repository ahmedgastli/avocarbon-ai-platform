package com.avocarbon.platform.module.generator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * MCP Client — connects to the local Model Context Protocol server via SSE.
 */
@Component
@Slf4j
public class McpClient {

    private final String mcpServerUrl;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public McpClient(
            @Value("${app.mcp.server-url:http://localhost:8081}") String mcpServerUrl,
            ObjectMapper objectMapper) {
        this.mcpServerUrl = mcpServerUrl;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public Map<String, String> generateAngularProject(String prompt,
                                                      ParsedOpenApiSpec spec,
                                                      GenerationType type) {
        log.info("Requesting Angular generation via MCP at '{}'", mcpServerUrl);

        try {
            // 1. Connect to SSE to get the message endpoint
            String messageEndpoint = establishSseConnectionAndGetEndpoint();
            if (messageEndpoint == null) {
                log.warn("Failed to get SSE endpoint from MCP server");
                return new LinkedHashMap<>();
            }
            log.info("Established SSE connection, message endpoint: {}", messageEndpoint);

            // 2. Prepare JSON-RPC request
            Map<String, Object> jsonRpcRequest = Map.of(
                    "jsonrpc", "2.0",
                    "id", UUID.randomUUID().toString(),
                    "method", "tools/call",
                    "params", Map.of(
                            "name", "generate_angular_project",
                            "arguments", Map.of(
                                    "prompt", prompt,
                                    "generationType", type.name()
                            )
                    )
            );
            String requestBody = objectMapper.writeValueAsString(jsonRpcRequest);

            // 3. Send the request
            HttpRequest postRequest = HttpRequest.newBuilder()
                    .uri(URI.create(messageEndpoint))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMinutes(5))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(postRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return parseMcpResponse(response.body());
            } else {
                log.error("MCP Server returned error: {} {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.warn("MCP server call failed: {}", e.getMessage());
        }

        return new LinkedHashMap<>();
    }

    private String establishSseConnectionAndGetEndpoint() throws Exception {
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(mcpServerUrl + "/sse"))
                .header("Accept", "text/event-stream")
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        CompletableFuture<String> endpointFuture = new CompletableFuture<>();

        // We use async send with line subscriber to read the SSE stream
        httpClient.sendAsync(getRequest, HttpResponse.BodyHandlers.ofLines())
                .thenAccept(response -> {
                    response.body().filter(line -> line.startsWith("event: endpoint") || line.startsWith("data: "))
                            .forEach(line -> {
                                if (line.startsWith("data: ") && !endpointFuture.isDone()) {
                                    String endpointPath = line.substring(6).trim();
                                    String fullUrl = mcpServerUrl + endpointPath;
                                    endpointFuture.complete(fullUrl);
                                }
                            });
                });

        // Wait up to 10 seconds for the endpoint to arrive
        return endpointFuture.get(10, TimeUnit.SECONDS);
    }

    private Map<String, String> parseMcpResponse(String json) {
        try {
            Map<String, Object> response = objectMapper.readValue(json, new TypeReference<>() {});
            
            // Check for JSON-RPC error
            if (response.containsKey("error")) {
                log.warn("MCP returned JSON-RPC error: {}", response.get("error"));
                return new LinkedHashMap<>();
            }

            Map<?, ?> result = (Map<?, ?>) response.get("result");
            if (result != null && result.get("content") instanceof List<?> contentList) {
                if (!contentList.isEmpty()) {
                    Map<?, ?> contentItem = (Map<?, ?>) contentList.get(0);
                    if ("text".equals(contentItem.get("type"))) {
                        String text = String.valueOf(contentItem.get("text"));
                        
                        // Try to parse the text as JSON Map<String, String>
                        return objectMapper.readValue(text, new TypeReference<>() {});
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse MCP response: {}", e.getMessage());
        }
        return new LinkedHashMap<>();
    }
}
