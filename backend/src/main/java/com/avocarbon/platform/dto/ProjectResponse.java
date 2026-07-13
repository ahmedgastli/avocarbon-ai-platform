package com.avocarbon.platform.dto;

import java.time.Instant;

import com.avocarbon.platform.entity.ProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object for project responses.
 *
 * Used to return project information to clients via REST API.
 * Hides internal entity structure and provides a clean API contract.
 *
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectResponse {

    private Long id;
    private String name;
    private String description;
    private ProjectStatus status;
    private Instant createdAt;
    private Instant updatedAt;

}
