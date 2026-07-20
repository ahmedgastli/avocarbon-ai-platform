package com.avocarbon.platform.module.integration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IntegrationSyncLogRepository extends JpaRepository<IntegrationSyncLog, Long> {
    List<IntegrationSyncLog> findByDataSourceId(Long dataSourceId);
    List<IntegrationSyncLog> findTop10ByDataSourceIdOrderBySyncTimeDesc(Long dataSourceId);
}
