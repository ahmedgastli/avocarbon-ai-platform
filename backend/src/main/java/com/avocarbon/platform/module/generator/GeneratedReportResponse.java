package com.avocarbon.platform.module.generator;

import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneratedReportResponse {
    private Long id;
    private Long projectId;
    private String projectName;
    private Long generatedById;
    private String generatedByName;
    private String title;
    private ReportType type;
    private String promptSummary;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;
}
