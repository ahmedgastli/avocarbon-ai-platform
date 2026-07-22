package com.avocarbon.platform.module.integration;

import com.avocarbon.platform.module.project.Project;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "data_sources", indexes = {
        @Index(name = "idx_datasource_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "API name is required")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank(message = "API URL is required")
    @Size(max = 500)
    @Column(nullable = false, length = 500)
    private String url;

    @NotBlank(message = "API Token is required")
    @Size(max = 1000)
    @Column(nullable = false, length = 1000)
    private String token;

    @NotNull(message = "API Type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DataSourceType type;

    @NotNull(message = "Sync frequency is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "sync_frequency", nullable = false, length = 20)
    private SyncFrequency syncFrequency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "site_id", length = 50)
    private String siteId;

    @OneToMany(mappedBy = "dataSource", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<IntegrationSyncLog> syncLogs = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "dataSource", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<ProductionMetric> productionMetrics = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "dataSource", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<QualityMetric> qualityMetrics = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "dataSource", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<MaintenanceMetric> maintenanceMetrics = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "dataSource", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<CustomerMetric> customerMetrics = new java.util.ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
