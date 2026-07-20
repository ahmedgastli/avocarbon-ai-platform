package com.avocarbon.platform.module.integration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ProductionMetricRepository extends JpaRepository<ProductionMetric, Long> {
    List<ProductionMetric> findByDataSourceId(Long dataSourceId);
    List<ProductionMetric> findByDataSourceIdAndTimestampBetween(Long dataSourceId, Instant start, Instant end);
    List<ProductionMetric> findByDataSourceProjectIdAndTimestampBetween(Long projectId, Instant start, Instant end);
}
