package com.avocarbon.platform.module.integration;

import com.avocarbon.platform.exception.ResourceNotFoundException;
import com.avocarbon.platform.module.project.Project;
import com.avocarbon.platform.module.project.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DataSourceServiceImpl implements DataSourceService {

    private final DataSourceRepository dataSourceRepository;
    private final ProjectRepository projectRepository;
    private final com.avocarbon.platform.module.identity.SecurityHelper securityHelper;

    @Override
    public DataSourceResponse createDataSource(Long projectId, DataSourceRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        securityHelper.validateSiteAccess(project.getSiteId());
        securityHelper.validateSiteAccess(request.getSiteId());

        DataSource dataSource = DataSource.builder()
                .name(request.getName())
                .url(request.getUrl())
                .token(request.getToken())
                .type(request.getType())
                .syncFrequency(request.getSyncFrequency())
                .project(project)
                .siteId(request.getSiteId())
                .build();

        DataSource saved = dataSourceRepository.save(dataSource);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DataSourceResponse> getDataSourcesByProjectId(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        securityHelper.validateSiteAccess(project.getSiteId());

        return dataSourceRepository.findByProjectId(projectId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DataSourceResponse getDataSourceById(Long id) {
        log.info("Fetching data source {}", id);
        DataSource dataSource = dataSourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Data source not found with id: " + id));

        securityHelper.validateSiteAccess(dataSource.getSiteId());

        return mapToResponse(dataSource);
    }

    @Override
    public DataSourceResponse updateDataSource(Long id, DataSourceRequest request) {
        DataSource dataSource = dataSourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Data source not found with id: " + id));

        securityHelper.validateSiteAccess(dataSource.getSiteId());
        securityHelper.validateSiteAccess(request.getSiteId());

        dataSource.setName(request.getName());
        dataSource.setUrl(request.getUrl());
        dataSource.setToken(request.getToken());
        dataSource.setType(request.getType());
        dataSource.setSyncFrequency(request.getSyncFrequency());
        dataSource.setSiteId(request.getSiteId());

        DataSource updated = dataSourceRepository.save(dataSource);
        return mapToResponse(updated);
    }

    @Override
    public void deleteDataSource(Long id) {
        log.info("Deleting data source {}", id);
        DataSource dataSource = dataSourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Data source not found with id: " + id));

        securityHelper.validateSiteAccess(dataSource.getSiteId());

        dataSourceRepository.delete(dataSource);
    }

    private DataSourceResponse mapToResponse(DataSource dataSource) {
        return DataSourceResponse.builder()
                .id(dataSource.getId())
                .name(dataSource.getName())
                .url(dataSource.getUrl())
                .token(dataSource.getToken())
                .type(dataSource.getType())
                .syncFrequency(dataSource.getSyncFrequency())
                .projectId(dataSource.getProject().getId())
                .projectName(dataSource.getProject().getName())
                .siteId(dataSource.getSiteId())
                .createdAt(dataSource.getCreatedAt())
                .updatedAt(dataSource.getUpdatedAt())
                .build();
    }
}
