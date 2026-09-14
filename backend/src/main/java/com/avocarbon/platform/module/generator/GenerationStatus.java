package com.avocarbon.platform.module.generator;

/**
 * Lifecycle states for a frontend generation job.
 *
 * PENDING  → job created, queued for processing
 * RUNNING  → generation pipeline is executing
 * SUCCESS  → ZIP is ready for download
 * FAILED   → pipeline error; errorMessage is populated
 */
public enum GenerationStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED
}
