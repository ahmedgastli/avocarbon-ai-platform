package com.avocarbon.platform.module.integration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface QualityMetricRepository extends JpaRepository<QualityMetric, Long> {
    List<QualityMetric> findByDataSourceId(Long dataSourceId);
    List<QualityMetric> findByDataSourceIdAndTimestampBetween(Long dataSourceId, Instant start, Instant end);
    List<QualityMetric> findByDataSourceProjectIdAndTimestampBetween(Long projectId, Instant start, Instant end);
}
