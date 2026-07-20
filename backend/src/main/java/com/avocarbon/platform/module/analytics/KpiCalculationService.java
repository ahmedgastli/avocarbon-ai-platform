package com.avocarbon.platform.module.analytics;

import com.avocarbon.platform.module.project.Project;
import com.avocarbon.platform.module.project.ProjectRepository;
import com.avocarbon.platform.module.integration.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class KpiCalculationService {

    private final ProjectRepository projectRepository;
    private final ProductionMetricRepository productionMetricRepository;
    private final QualityMetricRepository qualityMetricRepository;
    private final CustomerMetricRepository customerMetricRepository;
    private final MaintenanceMetricRepository maintenanceMetricRepository;
    private final KpiAggregationRepository kpiAggregationRepository;

    /**
     * Calculates KPIs for a project over a specified time window and saves the aggregation.
     */
    @Transactional
    public KpiAggregation calculateAndSaveKpi(Long projectId, Instant start, Instant end, String period) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        log.info("Calculating KPIs for project '{}' (Period: {}, Range: {} to {})", project.getName(), period, start, end);

        // Fetch metrics
        List<ProductionMetric> pmList = productionMetricRepository.findByDataSourceProjectIdAndTimestampBetween(projectId, start, end);
        List<QualityMetric> qmList = qualityMetricRepository.findByDataSourceProjectIdAndTimestampBetween(projectId, start, end);
        List<CustomerMetric> cmList = customerMetricRepository.findByDataSourceProjectIdAndTimestampBetween(projectId, start, end);
        List<MaintenanceMetric> mmList = maintenanceMetricRepository.findByDataSourceProjectIdAndTimestampBetween(projectId, start, end);

        // 1. Calculate OEE components
        double totalProduced = pmList.stream().mapToDouble(ProductionMetric::getQuantityProduced).sum();
        double totalTarget = pmList.stream().mapToDouble(ProductionMetric::getTargetQuantity).sum();
        double totalRunTime = pmList.stream().mapToDouble(ProductionMetric::getRunTimeMinutes).sum();
        double totalDownTime = pmList.stream().mapToDouble(ProductionMetric::getDownTimeMinutes).sum();

        double availability = 1.0;
        if ((totalRunTime + totalDownTime) > 0) {
            availability = totalRunTime / (totalRunTime + totalDownTime);
        }

        double performance = 1.0;
        if (totalTarget > 0) {
            performance = Math.min(1.0, totalProduced / totalTarget);
        }

        // 2. Calculate Quality & Scrap Rate
        double totalInspected = qmList.stream().mapToDouble(QualityMetric::getInspectedQuantity).sum();
        double totalDefective = qmList.stream().mapToDouble(QualityMetric::getDefectiveQuantity).sum();

        double quality = 1.0;
        double scrapRate = 0.0;
        if (totalInspected > 0) {
            scrapRate = totalDefective / totalInspected;
            quality = (totalInspected - totalDefective) / totalInspected;
        }

        // OEE calculation
        double oee = availability * performance * quality;

        // 3. Customer Satisfaction Average
        double customerSatisfaction = 90.0; // default benchmark
        if (!cmList.isEmpty()) {
            customerSatisfaction = cmList.stream().mapToDouble(CustomerMetric::getSatisfactionScore).average().orElse(90.0);
        }

        // 4. Maintenance Downtime
        double maintenanceDowntime = mmList.stream().mapToDouble(MaintenanceMetric::getDowntimeMinutes).sum();

        // Build and save KpiAggregation
        KpiAggregation aggregation = KpiAggregation.builder()
                .project(project)
                .calculationTimestamp(end)
                .aggregationPeriod(period)
                .availability(availability)
                .performance(performance)
                .quality(quality)
                .oee(oee)
                .scrapRate(scrapRate)
                .customerSatisfaction(customerSatisfaction)
                .maintenanceDowntime(maintenanceDowntime)
                .build();

        return kpiAggregationRepository.save(aggregation);
    }

    /**
     * Generates daily historical KPI entries for a project over the last 30 days.
     * This populates the KPI trend data.
     */
    @Transactional
    public void generateHistoricalKpis(Long projectId) {
        Instant now = Instant.now();
        
        // Clear existing aggregations to avoid duplicates on re-sync
        List<KpiAggregation> existing = kpiAggregationRepository.findByProjectId(projectId);
        kpiAggregationRepository.deleteAll(existing);

        // Generate DAILY data points for the past 30 days
        for (int i = 30; i >= 0; i--) {
            Instant dayStart = now.minus(i, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
            Instant dayEnd = dayStart.plus(1, ChronoUnit.DAYS).minus(1, ChronoUnit.MILLIS);
            calculateAndSaveKpi(projectId, dayStart, dayEnd, "DAILY");
        }

        // Generate OVERALL summary data point
        Instant thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);
        calculateAndSaveKpi(projectId, thirtyDaysAgo, now, "OVERALL");
    }
}
