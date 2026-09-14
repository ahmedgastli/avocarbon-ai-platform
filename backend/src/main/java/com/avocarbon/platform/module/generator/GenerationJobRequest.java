package com.avocarbon.platform.module.generator;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO for triggering a new frontend generation job.
 *
 * projectId is injected from the URL path variable by the controller
 * and must NOT be included in the JSON body.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerationJobRequest {

    @NotBlank(message = "Job name is required")
    @Size(max = 200, message = "Job name cannot exceed 200 characters")
    private String name;

    @NotNull(message = "Generation type is required")
    private GenerationType type;

    /**
     * Optional format hint: "JSON" or "YAML".
     * If null the parser will auto-detect from content.
     */
    @Size(max = 10)
    private String openApiFormat;

    @NotBlank(message = "OpenAPI specification content is required")
    @Size(max = 500_000, message = "OpenAPI specification cannot exceed 500 KB")
    private String openApiContent;
}
