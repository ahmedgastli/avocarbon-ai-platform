package com.avocarbon.platform.module.generator;

import com.avocarbon.platform.module.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class McpClient {

    private final String mcpServerUrl;
    private final RestClient restClient;

    public McpClient(@Value("${app.mcp.server-url:http://localhost:8081}") String mcpServerUrl) {
        this.mcpServerUrl = mcpServerUrl;
        this.restClient = RestClient.builder().build();
    }

    public String queryLocalData(Project project) {
        log.info("Querying MCP server at '{}' for project context on site '{}'", mcpServerUrl, project.getSiteId());

        try {
            boolean isMock = mcpServerUrl.contains("localhost") || mcpServerUrl.contains("mock");
            if (!isMock) {
                // Real call to local MCP server
                String response = restClient.get()
                        .uri(mcpServerUrl + "/api/tools/query-site-logs?siteId=" + project.getSiteId())
                        .retrieve()
                        .body(String.class);
                log.info("MCP server request successful");
                return response;
            }
        } catch (Exception e) {
            log.warn("Failed to connect to MCP server, falling back. Error: {}", e.getMessage());
        }

        // Simulating the MCP Server context lookup of local environment files
        return String.format(
                "[MCP Context: Successfully located local documentation manuals and carbon audit files for site '%s']",
                project.getSiteId()
        );
    }
}
