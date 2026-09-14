package com.avocarbon.platform.module.generator;

import com.avocarbon.platform.exception.ResourceNotFoundException;
import com.avocarbon.platform.module.identity.SecurityHelper;
import com.avocarbon.platform.module.identity.User;
import com.avocarbon.platform.module.project.Project;
import com.avocarbon.platform.module.project.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Transactional orchestrator for generation job management.
 *
 * Handles CRUD operations and delegates the heavy async pipeline
 * to {@link GenerationJobExecutor}.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class GeneratorServiceImpl implements GeneratorService {

    private final GenerationJobRepository jobRepository;
    private final ProjectRepository       projectRepository;
    private final SecurityHelper          securityHelper;
    private final GenerationJobExecutor   jobExecutor;
    private final FileStorageService      fileStorageService;

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @Override
    public GenerationJobResponse submitJob(Long projectId, GenerationJobRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
        securityHelper.validateSiteAccess(project.getSiteId());

        User user = securityHelper.getAuthenticatedUser();

        GenerationJob job = GenerationJob.builder()
                .project(project)
                .triggeredBy(user)
                .name(request.getName())
                .type(request.getType())
                .status(GenerationStatus.PENDING)
                .openApiFormat(request.getOpenApiFormat())
                .openApiContent(request.getOpenApiContent())
                .build();

        GenerationJob saved = jobRepository.save(job);
        log.info("Generation job {} created for project {}", saved.getId(), projectId);

        // Dispatch async pipeline — runs in generatorExecutor thread pool
        jobExecutor.executeAsync(saved.getId());

        return mapToResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<GenerationJobResponse> getJobsByProjectId(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
        securityHelper.validateSiteAccess(project.getSiteId());

        return jobRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GenerationJobResponse getJobById(Long id) {
        GenerationJob job = findJob(id);
        return mapToResponse(job);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadJobOutput(Long id) {
        GenerationJob job = findJob(id);

        if (job.getStatus() != GenerationStatus.SUCCESS || job.getOutputPath() == null) {
            throw new IllegalStateException(
                    "Generation job " + id + " output is not available (status: " + job.getStatus() + ")");
        }

        return fileStorageService.load(job.getOutputPath());
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    @Override
    public void deleteJob(Long id) {
        GenerationJob job = findJob(id);
        fileStorageService.delete(job.getOutputPath());
        jobRepository.delete(job);
        log.info("Generation job {} deleted", id);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private GenerationJob findJob(Long id) {
        GenerationJob job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Generation job not found with id: " + id));
        securityHelper.validateSiteAccess(job.getProject().getSiteId());
        return job;
    }

    private GenerationJobResponse mapToResponse(GenerationJob job) {
        return GenerationJobResponse.builder()
                .id(job.getId())
                .projectId(job.getProject().getId())
                .projectName(job.getProject().getName())
                .triggeredById(job.getTriggeredBy().getId())
                .triggeredByName(job.getTriggeredBy().getFirstName() + " " + job.getTriggeredBy().getLastName())
                .name(job.getName())
                .type(job.getType())
                .status(job.getStatus())
                .openApiFormat(job.getOpenApiFormat())
                .errorMessage(job.getErrorMessage())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}
