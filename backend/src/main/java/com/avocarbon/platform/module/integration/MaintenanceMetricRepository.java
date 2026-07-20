package com.avocarbon.platform.module.integration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MaintenanceMetricRepository extends JpaRepository<MaintenanceMetric, Long> {
    List<MaintenanceMetric> findByDataSourceId(Long dataSourceId);
    List<MaintenanceMetric> findByDataSourceIdAndTimestampBetween(Long dataSourceId, Instant start, Instant end);
    List<MaintenanceMetric> findByDataSourceProjectIdAndTimestampBetween(Long projectId, Instant start, Instant end);
}
