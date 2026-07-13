package com.avocarbon.platform.service;

import com.avocarbon.platform.dto.ProjectRequest;
import com.avocarbon.platform.dto.ProjectResponse;

import java.util.List;

/**
 * Service interface for project operations.
 *
 * Defines the business logic contract for managing projects.
 * Separates the service layer from implementation details.
 *
 * @since 1.0.0
 */
public interface ProjectService {

    /**
     * Create a new project.
     *
     * @param request the project creation request
     * @return the created project response
     */
    ProjectResponse createProject(ProjectRequest request);

    /**
     * Retrieve all projects.
     *
     * @return list of all projects
     */
    List<ProjectResponse> getAllProjects();

    /**
     * Retrieve a project by ID.
     *
     * @param id the project ID
     * @return the project response
     * @throws com.avocarbon.platform.exception.ResourceNotFoundException if project not found
     */
    ProjectResponse getProjectById(Long id);

    /**
     * Update an existing project.
     *
     * @param id the project ID
     * @param request the project update request
     * @return the updated project response
     * @throws com.avocarbon.platform.exception.ResourceNotFoundException if project not found
     */
    ProjectResponse updateProject(Long id, ProjectRequest request);

    /**
     * Delete a project by ID.
     *
     * @param id the project ID
     * @throws com.avocarbon.platform.exception.ResourceNotFoundException if project not found
     */
    void deleteProject(Long id);

}
