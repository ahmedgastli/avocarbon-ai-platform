package com.avocarbon.platform.module.generator;

import com.avocarbon.platform.module.identity.User;
import com.avocarbon.platform.module.project.Project;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * JPA entity representing one frontend-generation execution.
 *
 * A GenerationJob belongs to a Project and is triggered by a User.
 * It stores the raw OpenAPI specification, tracks its lifecycle
 * (PENDING → RUNNING → SUCCESS | FAILED) and, when successful,
 * records the path of the generated ZIP archive.
 *
 * @since 2.0.0
 */
@Entity
@Table(name = "generation_jobs", indexes = {
        @Index(name = "idx_job_project",   columnList = "project_id"),
        @Index(name = "idx_job_status",    columnList = "status"),
        @Index(name = "idx_job_triggered", columnList = "triggered_by_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerationJob {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @NotNull(message = "Project is required")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triggered_by_id", nullable = false)
    @NotNull(message = "Triggered-by user is required")
    private User triggeredBy;

    @NotBlank(message = "Job name is required")
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String name;

    @NotNull(message = "Generation type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GenerationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private GenerationStatus status = GenerationStatus.PENDING;

    /**
     * Format hint: "JSON" or "YAML" — auto-detected if null.
     */
    @Column(name = "openapi_format", length = 10)
    private String openApiFormat;

    /**
     * Raw OpenAPI specification provided by the user (JSON or YAML text).
     */
    @Lob
    @Column(name = "openapi_content", columnDefinition = "TEXT")
    private String openApiContent;

    /**
     * Absolute or relative path to the generated ZIP file on disk.
     * Null until status reaches SUCCESS.
     */
    @Column(name = "output_path", length = 500)
    private String outputPath;

    /**
     * Human-readable error description. Populated when status = FAILED.
     */
    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
