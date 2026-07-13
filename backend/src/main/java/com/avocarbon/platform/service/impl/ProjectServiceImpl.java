package com.avocarbon.platform.service.impl;

import com.avocarbon.platform.dto.ProjectRequest;
import com.avocarbon.platform.dto.ProjectResponse;
import com.avocarbon.platform.entity.Project;
import com.avocarbon.platform.entity.ProjectStatus;
import com.avocarbon.platform.exception.ResourceNotFoundException;
import com.avocarbon.platform.repository.ProjectRepository;
import com.avocarbon.platform.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for project operations.
 *
 * Implements the business logic for managing projects.
 * Handles entity to DTO mapping and repository interactions.
 *
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;

    @Override
    public ProjectResponse createProject(ProjectRequest request) {
        log.info("Creating new project with name: {}", request.getName());

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .status(ProjectStatus.CREATED)
                .build();

        Project savedProject = projectRepository.save(project);
        log.info("Project created successfully with id: {}", savedProject.getId());

        return mapToResponse(savedProject);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        log.info("Fetching all projects");
        return projectRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {
        log.info("Fetching project with id: {}", id);
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
        return mapToResponse(project);
    }

    @Override
    public ProjectResponse updateProject(Long id, ProjectRequest request) {
        log.info("Updating project with id: {}", id);

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));

        project.setName(request.getName());
        project.setDescription(request.getDescription());

        Project updatedProject = projectRepository.save(project);
        log.info("Project updated successfully with id: {}", updatedProject.getId());

        return mapToResponse(updatedProject);
    }

    @Override
    public void deleteProject(Long id) {
        log.info("Deleting project with id: {}", id);

        if (!projectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Project not found with id: " + id);
        }

        projectRepository.deleteById(id);
        log.info("Project deleted successfully with id: {}", id);
    }

    /**
     * Maps a Project entity to a ProjectResponse DTO.
     *
     * @param project the project entity
     * @return the project response DTO
     */
    private ProjectResponse mapToResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .status(project.getStatus())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

}
