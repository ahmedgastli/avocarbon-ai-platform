package com.avocarbon.platform.module.generator;

import java.util.List;

/**
 * Service interface for the Angular frontend Generator.
 *
 * Defines the public contract for job lifecycle management
 * and binary output retrieval.
 */
public interface GeneratorService {

    /**
     * Submit a new generation job for the given project.
     * The job is persisted with status PENDING and dispatched
     * asynchronously to the generation pipeline.
     *
     * @param projectId project to generate for
     * @param request   job parameters including the OpenAPI spec
     * @return created job response (status = PENDING)
     */
    GenerationJobResponse submitJob(Long projectId, GenerationJobRequest request);

    /**
     * List all generation jobs for a project, newest first.
     */
    List<GenerationJobResponse> getJobsByProjectId(Long projectId);

    /**
     * Fetch a single job by its ID.
     */
    GenerationJobResponse getJobById(Long id);

    /**
     * Load the raw ZIP bytes for a successfully completed job.
     *
     * @throws IllegalStateException if the job is not in SUCCESS status
     */
    byte[] downloadJobOutput(Long id);

    /**
     * Delete a job and its output file from disk.
     */
    void deleteJob(Long id);
}
