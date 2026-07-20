package com.avocarbon.platform.module.project;

import com.avocarbon.platform.module.project.ProjectStatus;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectResponse {

    private Long id;

    private String name;

    private String description;

    private ProjectStatus status;

    private Long ownerId;

    private String ownerName;

    private Instant startDate;

    private Instant createdAt;

    private Instant updatedAt;

}