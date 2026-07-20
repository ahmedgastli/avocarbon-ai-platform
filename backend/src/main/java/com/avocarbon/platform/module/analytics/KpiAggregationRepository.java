package com.avocarbon.platform.module.analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface KpiAggregationRepository extends JpaRepository<KpiAggregation, Long> {
    List<KpiAggregation> findByProjectId(Long projectId);
    List<KpiAggregation> findByProjectIdAndAggregationPeriodOrderByCalculationTimestampAsc(Long projectId, String aggregationPeriod);
    List<KpiAggregation> findByProjectIdAndAggregationPeriodAndCalculationTimestampBetween(Long projectId, String aggregationPeriod, Instant start, Instant end);
    Optional<KpiAggregation> findTopByProjectIdAndAggregationPeriodOrderByCalculationTimestampDesc(Long projectId, String aggregationPeriod);
}
