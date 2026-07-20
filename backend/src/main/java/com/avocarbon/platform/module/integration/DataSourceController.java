package com.avocarbon.platform.module.integration;

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
@Tag(name = "Data Sources", description = "Data Source / API Integration Endpoints")
public class DataSourceController {

    private final DataSourceService dataSourceService;
    private final SyncEngine syncEngine;

    @PostMapping("/datasources/{id}/sync")
    @Operation(summary = "Manually trigger sync", description = "Force sync for a data source immediately")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sync completed successfully"),
            @ApiResponse(responseCode = "404", description = "Data source not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Integer> syncDataSource(@PathVariable Long id) {
        log.info("Request to trigger sync manually for data source {}", id);
        int recordsImported = syncEngine.syncDataSource(id);
        return ResponseEntity.ok(recordsImported);
    }

    @PostMapping("/projects/{projectId}/datasources")
    @Operation(summary = "Add a data source to a project", description = "Register a new internal API connector for the project")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Data source added successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = DataSourceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<DataSourceResponse> createDataSource(
            @PathVariable Long projectId,
            @Valid @RequestBody DataSourceRequest request) {
        log.info("Request to create data source for project {}", projectId);
        DataSourceResponse response = dataSourceService.createDataSource(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/projects/{projectId}/datasources")
    @Operation(summary = "Get project data sources", description = "Retrieve all API integrations registered for a project")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Data sources retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = DataSourceResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<DataSourceResponse>> getDataSourcesByProjectId(@PathVariable Long projectId) {
        log.info("Request to list data sources for project {}", projectId);
        List<DataSourceResponse> responses = dataSourceService.getDataSourcesByProjectId(projectId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/datasources/{id}")
    @Operation(summary = "Get data source by ID", description = "Retrieve a specific data source configuration")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Data source retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = DataSourceResponse.class))),
            @ApiResponse(responseCode = "404", description = "Data source not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<DataSourceResponse> getDataSourceById(@PathVariable Long id) {
        log.info("Request to fetch data source {}", id);
        DataSourceResponse response = dataSourceService.getDataSourceById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/datasources/{id}")
    @Operation(summary = "Update a data source", description = "Update configuration of an existing data source")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Data source updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = DataSourceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "Data source not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<DataSourceResponse> updateDataSource(
            @PathVariable Long id,
            @Valid @RequestBody DataSourceRequest request) {
        log.info("Request to update data source {}", id);
        DataSourceResponse response = dataSourceService.updateDataSource(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/datasources/{id}")
    @Operation(summary = "Delete a data source", description = "Remove data source configuration")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Data source deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Data source not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> deleteDataSource(@PathVariable Long id) {
        log.info("Request to delete data source {}", id);
        dataSourceService.deleteDataSource(id);
        return ResponseEntity.noContent().build();
    }
}
