package com.avocarbon.platform.module.integration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface CustomerMetricRepository extends JpaRepository<CustomerMetric, Long> {
    List<CustomerMetric> findByDataSourceId(Long dataSourceId);
    List<CustomerMetric> findByDataSourceIdAndTimestampBetween(Long dataSourceId, Instant start, Instant end);
    List<CustomerMetric> findByDataSourceProjectIdAndTimestampBetween(Long projectId, Instant start, Instant end);
}
