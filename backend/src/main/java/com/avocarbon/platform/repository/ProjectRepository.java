package com.avocarbon.platform.repository;

import com.avocarbon.platform.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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

}
