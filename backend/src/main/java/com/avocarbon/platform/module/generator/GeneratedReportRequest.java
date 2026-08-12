package com.avocarbon.platform.module.generator;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneratedReportRequest {

    private Long projectId;

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    @NotNull(message = "Report type is required")
    private ReportType type;

    @Size(max = 2000, message = "Prompt summary cannot exceed 2000 characters")
    private String promptSummary;
}
