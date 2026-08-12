package com.avocarbon.platform.module.generator;

import com.avocarbon.platform.module.project.Project;
import com.avocarbon.platform.module.identity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "generated_reports", indexes = {
        @Index(name = "idx_report_project", columnList = "project_id"),
        @Index(name = "idx_report_generated_by", columnList = "generated_by_id"),
        @Index(name = "idx_report_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneratedReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @NotNull(message = "Project is required")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by_id", nullable = false)
    @NotNull(message = "Generator user is required")
    private User generatedBy;

    @NotBlank(message = "Title is required")
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @NotNull(message = "Report type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReportType type;

    @Size(max = 2000)
    @Column(name = "prompt_summary", length = 2000)
    private String promptSummary;

    @NotBlank(message = "Content cannot be blank")
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
