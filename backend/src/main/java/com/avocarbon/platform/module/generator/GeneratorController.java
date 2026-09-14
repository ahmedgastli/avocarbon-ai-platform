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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for the Angular Frontend Generator.
 *
 * Endpoint layout (mirrors DataSource convention):
 *   POST   /api/projects/{projectId}/generations   → submit job
 *   GET    /api/projects/{projectId}/generations   → list jobs
 *   GET    /api/generations/{id}                   → job details + status
 *   GET    /api/generations/{id}/download          → download ZIP
 *   DELETE /api/generations/{id}                   → delete job
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Generator", description = "Angular Frontend Generation Endpoints")
public class GeneratorController {

    private final GeneratorService generatorService;

    // -------------------------------------------------------------------------
    // POST /api/projects/{projectId}/generations
    // -------------------------------------------------------------------------

    @PostMapping("/projects/{projectId}/generations")
    @Operation(
        summary = "Submit a frontend generation job",
        description = "Trigger AI-powered Angular frontend generation for a project based on its OpenAPI specification. " +
                      "The job is queued asynchronously. Poll GET /api/generations/{id} for status updates."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Job queued (status = PENDING)",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = GenerationJobResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request or OpenAPI specification"),
        @ApiResponse(responseCode = "403", description = "Access denied for this site"),
        @ApiResponse(responseCode = "404", description = "Project not found")
    })
    public ResponseEntity<GenerationJobResponse> submitJob(
            @PathVariable Long projectId,
            @Valid @RequestBody GenerationJobRequest request) {
        log.info("Received generation request for project {}", projectId);
        GenerationJobResponse response = generatorService.submitJob(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // -------------------------------------------------------------------------
    // GET /api/projects/{projectId}/generations
    // -------------------------------------------------------------------------

    @GetMapping("/projects/{projectId}/generations")
    @Operation(summary = "List generation jobs for a project", description = "Returns all jobs ordered newest-first")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Jobs retrieved"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Project not found")
    })
    public ResponseEntity<List<GenerationJobResponse>> getJobsByProject(@PathVariable Long projectId) {
        log.info("Listing generation jobs for project {}", projectId);
        return ResponseEntity.ok(generatorService.getJobsByProjectId(projectId));
    }

    // -------------------------------------------------------------------------
    // GET /api/generations/{id}
    // -------------------------------------------------------------------------

    @GetMapping("/generations/{id}")
    @Operation(summary = "Get generation job status", description = "Poll this endpoint to check progress")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Job found",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = GenerationJobResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Job not found")
    })
    public ResponseEntity<GenerationJobResponse> getJobById(@PathVariable Long id) {
        log.info("Fetching generation job {}", id);
        return ResponseEntity.ok(generatorService.getJobById(id));
    }

    // -------------------------------------------------------------------------
    // GET /api/generations/{id}/download
    // -------------------------------------------------------------------------

    @GetMapping("/generations/{id}/download")
    @Operation(
        summary = "Download generated Angular project",
        description = "Returns the generated project as a ZIP archive. Only available when status = SUCCESS."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "ZIP file download"),
        @ApiResponse(responseCode = "400", description = "Job not yet completed or has failed"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Job not found")
    })
    public ResponseEntity<byte[]> downloadJob(@PathVariable Long id) {
        log.info("Download request for generation job {}", id);
        byte[] zipBytes = generatorService.downloadJobOutput(id);
        GenerationJobResponse job = generatorService.getJobById(id);
        String filename = job.getName().toLowerCase().replaceAll("[^a-z0-9]+", "-") + "-angular.zip";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(zipBytes.length)
                .body(zipBytes);
    }

    // -------------------------------------------------------------------------
    // DELETE /api/generations/{id}
    // -------------------------------------------------------------------------

    @DeleteMapping("/generations/{id}")
    @Operation(summary = "Delete a generation job", description = "Deletes the job record and its output ZIP file from disk")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Job deleted"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Job not found")
    })
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) {
        log.info("Deleting generation job {}", id);
        generatorService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }
}
