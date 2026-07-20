package com.avocarbon.platform.module.analytics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Analytics", description = "KPI and Dashboard Analytics Endpoints")
public class AnalyticsController {

    private final KpiAggregationRepository kpiAggregationRepository;
    private final KpiCalculationService kpiCalculationService;

    @GetMapping("/projects/{projectId}/summary")
    @Operation(summary = "Get overall KPI summary", description = "Retrieve the latest aggregated overall KPIs for a project")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "KPI summary retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Project not found or no data available")
    })
    public ResponseEntity<KpiAggregationResponse> getKpiSummary(@PathVariable Long projectId) {
        log.info("Request to fetch KPI summary for project {}", projectId);
        
        KpiAggregation latestOverall = kpiAggregationRepository
                .findTopByProjectIdAndAggregationPeriodOrderByCalculationTimestampDesc(projectId, "OVERALL")
                .orElseGet(() -> {
                    log.info("No OVERALL KPI aggregation found for project {}. Generating on the fly.", projectId);
                    try {
                        kpiCalculationService.generateHistoricalKpis(projectId);
                    } catch (Exception e) {
                        log.error("Failed to generate KPIs on the fly for project {}: {}", projectId, e.getMessage());
                        return null;
                    }
                    return kpiAggregationRepository
                            .findTopByProjectIdAndAggregationPeriodOrderByCalculationTimestampDesc(projectId, "OVERALL")
                            .orElse(null);
                });

        if (latestOverall == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(mapToResponse(latestOverall));
    }

    @GetMapping("/projects/{projectId}/history")
    @Operation(summary = "Get historical KPI trends", description = "Retrieve a list of daily aggregated KPIs for time-series charts")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "KPI history retrieved successfully")
    })
    public ResponseEntity<List<KpiAggregationResponse>> getKpiHistory(@PathVariable Long projectId) {
        log.info("Request to fetch KPI history for project {}", projectId);
        
        List<KpiAggregation> history = kpiAggregationRepository
                .findByProjectIdAndAggregationPeriodOrderByCalculationTimestampAsc(projectId, "DAILY");

        if (history.isEmpty()) {
            log.info("No historical KPIs found for project {}. Triggering generation.", projectId);
            try {
                kpiCalculationService.generateHistoricalKpis(projectId);
            } catch (Exception e) {
                log.error("Failed to generate historical KPIs for project {}: {}", projectId, e.getMessage());
            }
            history = kpiAggregationRepository
                    .findByProjectIdAndAggregationPeriodOrderByCalculationTimestampAsc(projectId, "DAILY");
        }

        List<KpiAggregationResponse> response = history.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    private KpiAggregationResponse mapToResponse(KpiAggregation entity) {
        return KpiAggregationResponse.builder()
                .id(entity.getId())
                .projectId(entity.getProject().getId())
                .projectName(entity.getProject().getName())
                .calculationTimestamp(entity.getCalculationTimestamp())
                .aggregationPeriod(entity.getAggregationPeriod())
                .availability(entity.getAvailability())
                .performance(entity.getPerformance())
                .quality(entity.getQuality())
                .oee(entity.getOee())
                .scrapRate(entity.getScrapRate())
                .customerSatisfaction(entity.getCustomerSatisfaction())
                .maintenanceDowntime(entity.getMaintenanceDowntime())
                .build();
    }
}
