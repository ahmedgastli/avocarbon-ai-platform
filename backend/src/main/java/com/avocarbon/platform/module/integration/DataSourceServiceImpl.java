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

    @Override
    public DataSourceResponse createDataSource(Long projectId, DataSourceRequest request) {
        log.info("Creating data source {} for project {}", request.getName(), projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        DataSource dataSource = DataSource.builder()
                .name(request.getName())
                .url(request.getUrl())
                .token(request.getToken())
                .type(request.getType())
                .syncFrequency(request.getSyncFrequency())
                .project(project)
                .build();

        DataSource saved = dataSourceRepository.save(dataSource);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DataSourceResponse> getDataSourcesByProjectId(Long projectId) {
        log.info("Fetching data sources for project {}", projectId);
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found with id: " + projectId);
        }
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
        return mapToResponse(dataSource);
    }

    @Override
    public DataSourceResponse updateDataSource(Long id, DataSourceRequest request) {
        log.info("Updating data source {}", id);
        DataSource dataSource = dataSourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Data source not found with id: " + id));

        dataSource.setName(request.getName());
        dataSource.setUrl(request.getUrl());
        dataSource.setToken(request.getToken());
        dataSource.setType(request.getType());
        dataSource.setSyncFrequency(request.getSyncFrequency());

        DataSource updated = dataSourceRepository.save(dataSource);
        return mapToResponse(updated);
    }

    @Override
    public void deleteDataSource(Long id) {
        log.info("Deleting data source {}", id);
        DataSource dataSource = dataSourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Data source not found with id: " + id));
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
                .createdAt(dataSource.getCreatedAt())
                .updatedAt(dataSource.getUpdatedAt())
                .build();
    }
}
