package com.avocarbon.platform.module.generator;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reports", description = "AI Report Generation Endpoints")
public class ReportGeneratorController {

    private final ReportGeneratorService reportGeneratorService;

    @PostMapping("/projects/{projectId}/reports")
    @Operation(summary = "Generate a new AI report", description = "Trigger AI report generation for a project using KPI aggregates")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Report generated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneratedReportResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "404", description = "Project not found or no KPI data"),
            @ApiResponse(responseCode = "403", description = "Access denied for this site")
    })
    public ResponseEntity<GeneratedReportResponse> generateReport(
            @PathVariable Long projectId,
            @Valid @RequestBody GeneratedReportRequest request) {
        log.info("Request received to generate report for project {}", projectId);
        request.setProjectId(projectId);
        GeneratedReportResponse response = reportGeneratorService.generateReport(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/projects/{projectId}/reports")
    @Operation(summary = "Get all project reports", description = "Retrieve all AI generated reports for a specific project")
    public ResponseEntity<List<GeneratedReportResponse>> getReportsByProject(@PathVariable Long projectId) {
        log.info("Request received to fetch reports for project {}", projectId);
        return ResponseEntity.ok(reportGeneratorService.getReportsByProjectId(projectId));
    }

    @GetMapping("/reports/{id}")
    @Operation(summary = "Get report details", description = "Fetch details of a specific generated report by its ID")
    public ResponseEntity<GeneratedReportResponse> getReportById(@PathVariable Long id) {
        log.info("Request received to fetch report {}", id);
        return ResponseEntity.ok(reportGeneratorService.getReportById(id));
    }

    @DeleteMapping("/reports/{id}")
    @Operation(summary = "Delete generated report", description = "Delete an AI generated report from history")
    public ResponseEntity<Void> deleteReport(@PathVariable Long id) {
        log.info("Request received to delete report {}", id);
        reportGeneratorService.deleteReport(id);
        return ResponseEntity.noContent().build();
    }
}
