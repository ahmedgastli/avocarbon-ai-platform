package com.avocarbon.platform.module.generator;

import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Builds a structured natural-language prompt for the LLM,
 * derived from a {@link ParsedOpenApiSpec} and the requested
 * {@link GenerationType}.
 *
 * The prompt is passed to {@link McpClient} (or {@link OpenAiClient})
 * to guide Angular code generation.
 */
@Component
public class PromptBuilder {

    public String buildPrompt(ParsedOpenApiSpec spec, GenerationType type) {
        StringBuilder sb = new StringBuilder();

        sb.append("You are an expert Angular 17+ developer using standalone components.\n");
        sb.append("Generate a complete Angular frontend application based on the OpenAPI specification below.\n\n");

        // Application metadata
        sb.append("## Application Details\n");
        sb.append("- **Title**: ").append(spec.getTitle()).append("\n");
        sb.append("- **Version**: ").append(spec.getVersion()).append("\n");
        if (spec.getDescription() != null && !spec.getDescription().isBlank()) {
            sb.append("- **Description**: ").append(spec.getDescription()).append("\n");
        }
        sb.append("- **Generation mode**: ").append(type).append("\n\n");

        // Resources
        sb.append("## Resources\n");
        if (spec.getResources().isEmpty()) {
            sb.append("No tagged resources detected — generate a generic CRUD scaffold.\n");
        } else {
            for (ParsedOpenApiSpec.ParsedResource resource : spec.getResources()) {
                sb.append("### ").append(resource.getName()).append("\n");
                for (ParsedOpenApiSpec.ParsedEndpoint ep : resource.getEndpoints()) {
                    sb.append("- `").append(ep.getMethod()).append(" ").append(ep.getPath()).append("`");
                    if (ep.getSummary() != null) sb.append(" — ").append(ep.getSummary());
                    if (ep.isHasRequestBody()) sb.append(" *(has request body)*");
                    sb.append("\n");
                }
            }
        }

        // Data models
        if (!spec.getSchemas().isEmpty()) {
            sb.append("\n## Data Models\n");
            for (ParsedOpenApiSpec.ParsedSchema schema : spec.getSchemas()) {
                sb.append("### ").append(schema.getName()).append("\n");
                schema.getProperties().forEach((prop, openApiType) ->
                    sb.append("- `").append(prop).append("`: ").append(openApiType).append("\n"));
            }
        }

        // Generation instructions
        sb.append("\n## Generation Instructions\n");
        sb.append("- Use Angular 17+ **standalone components** (no NgModules).\n");
        sb.append("- Use **HttpClient** (injected via `inject()`) for all API calls.\n");
        sb.append("- Use **Reactive Forms** for all forms.\n");
        sb.append("- Base API URL: use `/api` as the prefix unless the spec specifies servers.\n\n");

        appendTypeInstructions(sb, type);

        sb.append("\n## Output Format\n");
        sb.append("Return ONLY a JSON object where each key is a relative file path and the value is the complete file content:\n");
        sb.append("```json\n{ \"src/app/app.component.ts\": \"...\", ... }\n```\n");

        return sb.toString();
    }

    private void appendTypeInstructions(StringBuilder sb, GenerationType type) {
        switch (type) {
            case ANGULAR_FULL -> {
                sb.append("### Files to generate (ANGULAR_FULL)\n");
                sb.append("- `src/app/app.config.ts` — providers: router + httpClient\n");
                sb.append("- `src/app/app.routes.ts` — lazy-loaded routes per resource\n");
                sb.append("- `src/app/app.component.ts` + `.html` — shell with nav + router-outlet\n");
                sb.append("- Per resource: model interface, HttpClient service, list component, form component\n");
            }
            case ANGULAR_CRUD -> {
                sb.append("### Files to generate (ANGULAR_CRUD)\n");
                sb.append("- Per resource: TypeScript model interface, HttpClient service, list component\n");
                sb.append("- No routing or app shell required\n");
            }
            case ANGULAR_FORMS -> {
                sb.append("### Files to generate (ANGULAR_FORMS)\n");
                sb.append("- Per resource: TypeScript model interface, reactive form component with validation\n");
                sb.append("- No routing or list views required\n");
            }
        }
    }
}
