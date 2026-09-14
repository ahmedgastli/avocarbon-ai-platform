package com.avocarbon.platform.module.generator;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Parses and validates an OpenAPI specification (JSON or YAML) using
 * the swagger-parser library (already on the classpath via pom.xml).
 *
 * Produces a {@link ParsedOpenApiSpec} consumed by {@link PromptBuilder}
 * and {@link McpClient}.
 */
@Component
@Slf4j
public class OpenApiParser {

    /**
     * Parse raw OpenAPI content into a structured model.
     *
     * @param content   raw JSON or YAML text
     * @param format    optional hint "JSON" or "YAML"; auto-detected if null
     * @throws IllegalArgumentException if the spec is invalid or empty
     */
    public ParsedOpenApiSpec parse(String content, String format) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("OpenAPI specification content cannot be empty");
        }

        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(content, null, options);

        if (result.getOpenAPI() == null) {
            String errors = (result.getMessages() != null && !result.getMessages().isEmpty())
                    ? String.join("; ", result.getMessages())
                    : "Unknown parse error";
            log.warn("OpenAPI parse failed: {}", errors);
            throw new IllegalArgumentException("Invalid OpenAPI specification: " + errors);
        }

        log.info("OpenAPI specification parsed successfully");
        return buildSpec(result.getOpenAPI());
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private ParsedOpenApiSpec buildSpec(OpenAPI openAPI) {
        Info info = openAPI.getInfo();
        String title       = info != null && info.getTitle()       != null ? info.getTitle()       : "Generated Application";
        String version     = info != null && info.getVersion()     != null ? info.getVersion()     : "1.0.0";
        String description = info != null && info.getDescription() != null ? info.getDescription() : "";

        List<ParsedOpenApiSpec.ParsedEndpoint> endpoints = new ArrayList<>();
        Map<String, List<ParsedOpenApiSpec.ParsedEndpoint>> byTag = new LinkedHashMap<>();

        if (openAPI.getPaths() != null) {
            for (Map.Entry<String, PathItem> pathEntry : openAPI.getPaths().entrySet()) {
                String path     = pathEntry.getKey();
                PathItem item   = pathEntry.getValue();
                extractOp(path, "GET",    item.getGet(),    endpoints, byTag);
                extractOp(path, "POST",   item.getPost(),   endpoints, byTag);
                extractOp(path, "PUT",    item.getPut(),    endpoints, byTag);
                extractOp(path, "DELETE", item.getDelete(), endpoints, byTag);
                extractOp(path, "PATCH",  item.getPatch(),  endpoints, byTag);
            }
        }

        // Build resource list from tag groups (or path-prefix groups)
        List<ParsedOpenApiSpec.ParsedResource> resources;
        if (!byTag.isEmpty()) {
            resources = byTag.entrySet().stream()
                    .map(e -> ParsedOpenApiSpec.ParsedResource.builder()
                            .name(e.getKey())
                            .endpoints(e.getValue())
                            .build())
                    .toList();
        } else {
            resources = groupByPathPrefix(endpoints);
        }

        // Extract schemas from components
        List<ParsedOpenApiSpec.ParsedSchema> schemas = new ArrayList<>();
        if (openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null) {
            for (Map.Entry<String, Schema> entry : openAPI.getComponents().getSchemas().entrySet()) {
                schemas.add(extractSchema(entry.getKey(), entry.getValue()));
            }
        }

        log.info("Parsed spec '{}' v{} — {} resources, {} endpoints, {} schemas",
                title, version, resources.size(), endpoints.size(), schemas.size());

        return ParsedOpenApiSpec.builder()
                .title(title)
                .version(version)
                .description(description)
                .resources(resources)
                .endpoints(endpoints)
                .schemas(schemas)
                .build();
    }

    private void extractOp(String path, String method, Operation op,
                           List<ParsedOpenApiSpec.ParsedEndpoint> endpoints,
                           Map<String, List<ParsedOpenApiSpec.ParsedEndpoint>> byTag) {
        if (op == null) return;

        List<String> tags = (op.getTags() != null && !op.getTags().isEmpty())
                ? op.getTags()
                : new ArrayList<>();

        ParsedOpenApiSpec.ParsedEndpoint endpoint = ParsedOpenApiSpec.ParsedEndpoint.builder()
                .method(method)
                .path(path)
                .operationId(op.getOperationId())
                .summary(op.getSummary())
                .tags(new ArrayList<>(tags))
                .hasRequestBody(op.getRequestBody() != null)
                .build();

        endpoints.add(endpoint);

        if (!tags.isEmpty()) {
            // Group by primary tag, normalised to lower-kebab-case
            String key = tags.get(0).toLowerCase().replaceAll("[^a-z0-9]+", "-");
            byTag.computeIfAbsent(key, k -> new ArrayList<>()).add(endpoint);
        }
    }

    private List<ParsedOpenApiSpec.ParsedResource> groupByPathPrefix(
            List<ParsedOpenApiSpec.ParsedEndpoint> endpoints) {
        Map<String, List<ParsedOpenApiSpec.ParsedEndpoint>> byPrefix = new LinkedHashMap<>();
        for (ParsedOpenApiSpec.ParsedEndpoint ep : endpoints) {
            String[] parts = ep.getPath().split("/");
            String prefix  = parts.length > 1 ? parts[1] : "root";
            byPrefix.computeIfAbsent(prefix, k -> new ArrayList<>()).add(ep);
        }
        return byPrefix.entrySet().stream()
                .map(e -> ParsedOpenApiSpec.ParsedResource.builder()
                        .name(e.getKey())
                        .endpoints(e.getValue())
                        .build())
                .toList();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private ParsedOpenApiSpec.ParsedSchema extractSchema(String name, Schema<?> schema) {
        Map<String, String> props = new LinkedHashMap<>();
        if (schema.getProperties() != null) {
            ((Map<String, Schema>) schema.getProperties()).forEach((propName, propSchema) -> {
                String type = propSchema.getType() != null ? propSchema.getType() : "object";
                props.put(propName, type);
            });
        }
        return ParsedOpenApiSpec.ParsedSchema.builder()
                .name(name)
                .properties(props)
                .build();
    }
}
