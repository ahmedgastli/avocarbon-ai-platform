package com.avocarbon.platform.module.generator;

import java.util.List;

public interface ReportGeneratorService {
    GeneratedReportResponse generateReport(GeneratedReportRequest request);
    List<GeneratedReportResponse> getReportsByProjectId(Long projectId);
    GeneratedReportResponse getReportById(Long id);
    void deleteReport(Long id);
}
