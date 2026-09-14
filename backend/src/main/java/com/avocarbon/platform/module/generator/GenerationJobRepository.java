package com.avocarbon.platform.module.generator;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for GenerationJob entities.
 *
 * Provides CRUD operations and project-scoped queries.
 */
@Repository
public interface GenerationJobRepository extends JpaRepository<GenerationJob, Long> {

    List<GenerationJob> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<GenerationJob> findByProjectIdAndStatus(Long projectId, GenerationStatus status);

    List<GenerationJob> findByTriggeredByIdOrderByCreatedAtDesc(Long userId);
}
