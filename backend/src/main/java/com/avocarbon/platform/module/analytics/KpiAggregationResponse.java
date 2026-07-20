package com.avocarbon.platform.module.analytics;

import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KpiAggregationResponse {
    private Long id;
    private Long projectId;
    private String projectName;
    private Instant calculationTimestamp;
    private String aggregationPeriod;
    private Double scrapRate;
    private Double availability;
    private Double performance;
    private Double quality;
    private Double oee;
    private Double customerSatisfaction;
    private Double maintenanceDowntime;
}
