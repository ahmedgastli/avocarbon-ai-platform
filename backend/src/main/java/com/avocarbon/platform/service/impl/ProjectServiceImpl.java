package com.avocarbon.platform.service.impl;

import com.avocarbon.platform.dto.ProjectRequest;
import com.avocarbon.platform.dto.ProjectResponse;
import com.avocarbon.platform.entity.Project;
import com.avocarbon.platform.entity.ProjectStatus;
import com.avocarbon.platform.entity.User;
import com.avocarbon.platform.exception.ResourceNotFoundException;
import com.avocarbon.platform.repository.ProjectRepository;
import com.avocarbon.platform.repository.UserRepository;
import com.avocarbon.platform.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Override
    public ProjectResponse createProject(ProjectRequest request) {

        log.info("Creating project {}", request.getName());

        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id : " + request.getOwnerId()
                        ));

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .status(ProjectStatus.CREATED)
                .owner(owner)
                .build();

        Project saved = projectRepository.save(project);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {

        return projectRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {

        Project project = projectRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Project not found with id : " + id
                        ));

        return mapToResponse(project);
    }

    @Override
    public ProjectResponse updateProject(Long id, ProjectRequest request) {

        Project project = projectRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Project not found with id : " + id
                        ));

        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id : " + request.getOwnerId()
                        ));

        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setOwner(owner);

        Project updated = projectRepository.save(project);

        return mapToResponse(updated);
    }

    @Override
    public void deleteProject(Long id) {

        Project project = projectRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Project not found with id : " + id
                        ));

        projectRepository.delete(project);
    }

    private ProjectResponse mapToResponse(Project project) {

        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .status(project.getStatus())
                .ownerId(project.getOwner().getId())
                .ownerName(
                        project.getOwner().getFirstName()
                                + " "
                                + project.getOwner().getLastName()
                )
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

}