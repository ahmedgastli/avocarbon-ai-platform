package com.avocarbon.platform.module.integration;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "production_metrics", indexes = {
        @Index(name = "idx_prod_metric_datasource", columnList = "datasource_id"),
        @Index(name = "idx_prod_metric_timestamp", columnList = "timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionMetric {

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

    @Column(name = "quantity_produced", nullable = false)
    private Double quantityProduced;

    @Column(name = "target_quantity", nullable = false)
    private Double targetQuantity;

    @Column(name = "run_time_minutes", nullable = false)
    private Double runTimeMinutes;

    @Column(name = "down_time_minutes", nullable = false)
    private Double downTimeMinutes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
