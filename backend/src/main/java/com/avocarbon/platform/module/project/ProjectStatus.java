package com.avocarbon.platform.module.project;

/**
 * Project status enumeration representing the lifecycle of a project.
 *
 * - CREATED: Project has been created but not yet processed
 * - IN_PROGRESS: Project is currently being processed (OpenAPI parsing, code generation, etc.)
 * - GENERATED: Project code has been successfully generated
 * - DEPLOYED: Project has been deployed to the target environment
 * - FAILED: Project processing failed at some stage
 *
 * @since 1.0.0
 */
public enum ProjectStatus {
    CREATED,
    IN_PROGRESS,
    GENERATED,
    DEPLOYED,
    FAILED
}
