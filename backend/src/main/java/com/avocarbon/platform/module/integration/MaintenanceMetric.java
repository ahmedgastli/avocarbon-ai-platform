package com.avocarbon.platform.module.integration;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "maintenance_metrics", indexes = {
        @Index(name = "idx_maint_metric_datasource", columnList = "datasource_id"),
        @Index(name = "idx_maint_metric_timestamp", columnList = "timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datasource_id", nullable = false)
    @NotNull(message = "DataSource is required")
    private DataSource dataSource;

    @Column(nullable = false)
    @NotNull(message = "Timestamp is required")
    private Instant timestamp;

    @Column(name = "downtime_minutes", nullable = false)
    private Double downtimeMinutes;

    @Column(name = "maintenance_type", length = 50)
    private String maintenanceType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
