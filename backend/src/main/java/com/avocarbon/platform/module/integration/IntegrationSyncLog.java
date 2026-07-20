package com.avocarbon.platform.module.integration;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "integration_sync_logs", indexes = {
        @Index(name = "idx_sync_log_datasource", columnList = "datasource_id"),
        @Index(name = "idx_sync_log_time", columnList = "sync_time")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegrationSyncLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datasource_id", nullable = false)
    @NotNull(message = "DataSource is required")
    private DataSource dataSource;

    @Column(name = "sync_time", nullable = false)
    @NotNull(message = "Sync time is required")
    private Instant syncTime;

    @Column(nullable = false, length = 20)
    private String status; // "SUCCESS", "FAILED"

    @Column(name = "records_imported")
    private Integer recordsImported;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;
}
