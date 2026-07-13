package com.avocarbon.platform.entity;

import java.time.Instant;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Project entity representing a project in the AVOCarbon AI Platform.
 *
 * A project encapsulates an OpenAPI specification and the generated application.
 * It tracks the project lifecycle from creation through deployment.
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "projects", indexes = {
    @Index(name = "idx_project_status", columnList = "status"),
    @Index(name = "idx_project_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Project name is required")
    @Size(min = 3, max = 100, message = "Project name must be between 3 and 100 characters")
    @Column(nullable = false, length = 100)
    private String name;

    @Size(max = 1000, message = "Project description must not exceed 1000 characters")
    @Column(length = 1000)
    private String description;

    @NotNull(message = "Project status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

}
