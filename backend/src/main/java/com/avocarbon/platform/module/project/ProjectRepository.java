package com.avocarbon.platform.module.project;

import com.avocarbon.platform.module.project.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Project entity.
 *
 * Provides CRUD operations and database access for Project entities.
 * Extends JpaRepository to inherit common repository methods.
 *
 * @since 1.0.0
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByOwnerId(Long ownerId);

}
