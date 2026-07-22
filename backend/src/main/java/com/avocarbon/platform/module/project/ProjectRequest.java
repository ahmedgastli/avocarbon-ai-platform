package com.avocarbon.platform.module.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectRequest {

    @NotBlank(message = "Project name is required")
    @Size(min = 3, max = 100)
    private String name;

    @Size(max = 1000)
    private String description;

    @NotNull(message = "Owner is required")
    private Long ownerId;

    @NotNull(message = "Start date is required")
    private Instant startDate;

    @NotBlank(message = "Site is required")
    private String siteId;

}