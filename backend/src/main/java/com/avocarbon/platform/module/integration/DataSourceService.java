package com.avocarbon.platform.module.integration;

import java.util.List;

public interface DataSourceService {
    DataSourceResponse createDataSource(Long projectId, DataSourceRequest request);
    List<DataSourceResponse> getDataSourcesByProjectId(Long projectId);
    DataSourceResponse getDataSourceById(Long id);
    DataSourceResponse updateDataSource(Long id, DataSourceRequest request);
    void deleteDataSource(Long id);
}
