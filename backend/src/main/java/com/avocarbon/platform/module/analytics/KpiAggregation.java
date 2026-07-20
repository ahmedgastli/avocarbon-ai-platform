package com.avocarbon.platform.module.analytics;

import com.avocarbon.platform.module.project.Project;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "kpi_aggregations", indexes = {
        @Index(name = "idx_kpi_proj", columnList = "project_id"),
        @Index(name = "idx_kpi_period", columnList = "aggregation_period"),
        @Index(name = "idx_kpi_time", columnList = "calculation_timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KpiAggregation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @NotNull(message = "Project is required")
    private Project project;

    @Column(name = "calculation_timestamp", nullable = false)
    @NotNull(message = "Calculation timestamp is required")
    private Instant calculationTimestamp;

    @Column(name = "aggregation_period", nullable = false, length = 20)
    @NotNull(message = "Aggregation period is required")
    private String aggregationPeriod; // "DAILY", "WEEKLY", "MONTHLY", "OVERALL"

    @Column(name = "scrap_rate")
    private Double scrapRate;

    private Double availability;
    private Double performance;
    private Double quality;
    private Double oee;

    @Column(name = "customer_satisfaction")
    private Double customerSatisfaction;

    @Column(name = "maintenance_downtime")
    private Double maintenanceDowntime;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
