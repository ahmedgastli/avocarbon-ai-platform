package com.avocarbon.platform.module.generator;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GeneratedReportRepository extends JpaRepository<GeneratedReport, Long> {
    List<GeneratedReport> findByProjectIdOrderByCreatedAtDesc(Long projectId);
    List<GeneratedReport> findByGeneratedByIdOrderByCreatedAtDesc(Long userId);
}
