package com.avocarbon.platform.module.project;

import com.avocarbon.platform.module.project.ProjectRequest;
import com.avocarbon.platform.module.project.ProjectResponse;
import com.avocarbon.platform.module.project.ProjectService;
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

/**
 * REST Controller for project management.
 *
 * Handles HTTP requests related to project operations.
 * All endpoints return ResponseEntity with appropriate HTTP status codes.
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Projects", description = "Project Management Endpoints")
public class ProjectController {

    private final ProjectService projectService;

    /**
     * Create a new project.
     *
     * @param request the project creation request
     * @return created project response with 201 status
     */
    @PostMapping
    @Operation(summary = "Create a new project", description = "Create a new project with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Project created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProjectResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectRequest request) {
        log.info("Creating new project");
        ProjectResponse response = projectService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieve all projects.
     *
     * @return list of all projects with 200 status
     */
    @GetMapping
    @Operation(summary = "Get all projects", description = "Retrieve a list of all projects")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Projects retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProjectResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        log.info("Fetching all projects");
        List<ProjectResponse> projects = projectService.getAllProjects();
        return ResponseEntity.ok(projects);
    }

    /**
     * Retrieve a project by ID.
     *
     * @param id the project ID
     * @return project response with 200 status
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get project by ID", description = "Retrieve a specific project by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Project found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProjectResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long id) {
        log.info("Fetching project with id: {}", id);
        ProjectResponse project = projectService.getProjectById(id);
        return ResponseEntity.ok(project);
    }

    /**
     * Update an existing project.
     *
     * @param id the project ID
     * @param request the project update request
     * @return updated project response with 200 status
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update a project", description = "Update an existing project with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Project updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProjectResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequest request) {
        log.info("Updating project with id: {}", id);
        ProjectResponse response = projectService.updateProject(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a project by ID.
     *
     * @param id the project ID
     * @return 204 No Content status
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a project", description = "Delete a project by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Project deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        log.info("Deleting project with id: {}", id);
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

}
