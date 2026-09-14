package com.avocarbon.platform.module.generator;

import lombok.Builder;
import lombok.Getter;

import java.util.*;

/**
 * Immutable value object produced by OpenApiParser.
 *
 * Carries the structured information extracted from a raw OpenAPI
 * JSON/YAML document: title, version, per-tag resource groups,
 * flat endpoint list, and named schema definitions.
 *
 * All downstream components (PromptBuilder, McpClient,
 * AngularProjectGenerator) consume this object.
 */
@Getter
@Builder
public class ParsedOpenApiSpec {

    private final String title;
    private final String version;
    private final String description;

    @Builder.Default
    private final List<ParsedResource> resources = new ArrayList<>();

    @Builder.Default
    private final List<ParsedEndpoint> endpoints = new ArrayList<>();

    @Builder.Default
    private final List<ParsedSchema> schemas = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Nested types
    // -------------------------------------------------------------------------

    @Getter
    @Builder
    public static class ParsedResource {
        /** Tag or path-prefix name, lower-kebab-case (e.g. "pet-store", "users"). */
        private final String name;

        @Builder.Default
        private final List<ParsedEndpoint> endpoints = new ArrayList<>();
    }

    @Getter
    @Builder
    public static class ParsedEndpoint {
        /** HTTP method in upper case: GET, POST, PUT, DELETE, PATCH. */
        private final String method;
        /** Raw path string as declared in OpenAPI, e.g. "/pets/{petId}". */
        private final String path;
        private final String operationId;
        private final String summary;
        @Builder.Default
        private final List<String> tags = new ArrayList<>();
        private final boolean hasRequestBody;
        private final String responseSchemaName;
    }

    @Getter
    @Builder
    public static class ParsedSchema {
        /** Schema name as declared in components/schemas (Pascal-case). */
        private final String name;
        /** Property name → OpenAPI type string (string, integer, boolean, array, object). */
        @Builder.Default
        private final Map<String, String> properties = new LinkedHashMap<>();
    }
}
