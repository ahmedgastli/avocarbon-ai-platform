package com.avocarbon.platform.module.integration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataSourceRequest {

    @NotBlank(message = "API name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "API URL is required")
    @Size(max = 500)
    private String url;

    @NotBlank(message = "API Token is required")
    @Size(max = 1000)
    private String token;

    @NotNull(message = "API Type is required")
    private DataSourceType type;

    @NotNull(message = "Sync frequency is required")
    private SyncFrequency syncFrequency;

    @jakarta.validation.constraints.NotBlank(message = "Site is required")
    private String siteId;
}
