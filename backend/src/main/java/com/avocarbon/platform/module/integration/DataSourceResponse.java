package com.avocarbon.platform.module.integration;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataSourceResponse {

    private Long id;
    private String name;
    private String url;
    private String token;
    private DataSourceType type;
    private SyncFrequency syncFrequency;
    private Long projectId;
    private String projectName;
    private Instant createdAt;
    private Instant updatedAt;
}
