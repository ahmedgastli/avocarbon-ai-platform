package com.avocarbon.platform.module.generator;

import lombok.*;
import java.time.Instant;

/**
 * DTO returned for all generation job API responses.
 *
 * outputPath is intentionally excluded from the response
 * (it is a server-internal filesystem path).
 * The download link is accessed via GET /api/generations/{id}/download.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerationJobResponse {
    private Long id;
    private Long projectId;
    private String projectName;
    private Long triggeredById;
    private String triggeredByName;
    private String name;
    private GenerationType type;
    private GenerationStatus status;
    private String openApiFormat;
    private String errorMessage;
    private Instant startedAt;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
