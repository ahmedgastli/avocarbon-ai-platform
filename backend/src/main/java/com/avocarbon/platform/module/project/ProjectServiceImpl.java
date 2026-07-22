package com.avocarbon.platform.module.project;

import com.avocarbon.platform.module.project.ProjectRequest;
import com.avocarbon.platform.module.project.ProjectResponse;
import com.avocarbon.platform.module.project.Project;
import com.avocarbon.platform.module.project.ProjectStatus;
import com.avocarbon.platform.module.identity.User;
import com.avocarbon.platform.exception.ResourceNotFoundException;
import com.avocarbon.platform.module.project.ProjectRepository;
import com.avocarbon.platform.module.identity.UserRepository;
import com.avocarbon.platform.module.project.ProjectService;
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
    private final com.avocarbon.platform.module.identity.SecurityHelper securityHelper;

    @Override
    public ProjectResponse createProject(ProjectRequest request) {
        securityHelper.validateSiteAccess(request.getSiteId());

        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id : " + request.getOwnerId()
                        ));

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .status(ProjectStatus.CREATED)
                .startDate(request.getStartDate())
                .owner(owner)
                .siteId(request.getSiteId())
                .build();

        Project saved = projectRepository.save(project);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {

        return projectRepository.findAll()
                .stream()
                .filter(p -> securityHelper.isSiteAuthorized(p.getSiteId()))
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

        securityHelper.validateSiteAccess(project.getSiteId());

        return mapToResponse(project);
    }

    @Override
    public ProjectResponse updateProject(Long id, ProjectRequest request) {

        Project project = projectRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Project not found with id : " + id
                        ));

        securityHelper.validateSiteAccess(project.getSiteId());
        securityHelper.validateSiteAccess(request.getSiteId());

        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id : " + request.getOwnerId()
                        ));

        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setStartDate(request.getStartDate());
        project.setOwner(owner);
        project.setSiteId(request.getSiteId());

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

        securityHelper.validateSiteAccess(project.getSiteId());

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
                .startDate(project.getStartDate())
                .siteId(project.getSiteId())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

}