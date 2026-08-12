package com.avocarbon.platform.module.generator;

import com.avocarbon.platform.module.project.Project;
import com.avocarbon.platform.module.project.ProjectRepository;
import com.avocarbon.platform.module.identity.User;
import com.avocarbon.platform.module.identity.SecurityHelper;
import com.avocarbon.platform.module.analytics.KpiAggregation;
import com.avocarbon.platform.module.analytics.KpiAggregationRepository;
import com.avocarbon.platform.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReportGeneratorServiceImpl implements ReportGeneratorService {

    private final GeneratedReportRepository reportRepository;
    private final ProjectRepository projectRepository;
    private final KpiAggregationRepository kpiAggregationRepository;
    private final SecurityHelper securityHelper;
    private final OpenAiClient openAiClient;
    private final McpClient mcpClient;

    @Override
    public GeneratedReportResponse generateReport(GeneratedReportRequest request) {
        log.info("Starting report generation for project {}", request.getProjectId());

        // 1. Fetch project and validate user's site authorization
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id : " + request.getProjectId()));
        securityHelper.validateSiteAccess(project.getSiteId());

        // 2. Fetch authenticated user
        User user = securityHelper.getAuthenticatedUser();

        // 3. Fetch consolidated KPI context
        KpiAggregation kpiSummary = kpiAggregationRepository
                .findTopByProjectIdAndAggregationPeriodOrderByCalculationTimestampDesc(project.getId(), "OVERALL")
                .orElseThrow(() -> new ResourceNotFoundException("No consolidated KPIs found for project: " + project.getId()));

        // 4. Contact MCP Server for additional tool data/logs (if available)
        String mcpContext = mcpClient.queryLocalData(project);

        // 5. Connect to Azure OpenAI to synthesize report content
        String aiResponse = openAiClient.synthesizeReport(request.getType(), project, kpiSummary, mcpContext, request.getPromptSummary());

        // 6. Build and save entity
        GeneratedReport report = GeneratedReport.builder()
                .project(project)
                .generatedBy(user)
                .title(request.getTitle())
                .type(request.getType())
                .promptSummary(request.getPromptSummary())
                .content(aiResponse)
                .build();

        GeneratedReport saved = reportRepository.save(report);
        log.info("Report generated successfully with ID {}", saved.getId());

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GeneratedReportResponse> getReportsByProjectId(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id : " + projectId));
        securityHelper.validateSiteAccess(project.getSiteId());

        return reportRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GeneratedReportResponse getReportById(Long id) {
        GeneratedReport report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id : " + id));
        securityHelper.validateSiteAccess(report.getProject().getSiteId());

        return mapToResponse(report);
    }

    @Override
    public void deleteReport(Long id) {
        GeneratedReport report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id : " + id));
        securityHelper.validateSiteAccess(report.getProject().getSiteId());

        reportRepository.delete(report);
        log.info("Report {} deleted successfully", id);
    }

    private GeneratedReportResponse mapToResponse(GeneratedReport report) {
        return GeneratedReportResponse.builder()
                .id(report.getId())
                .projectId(report.getProject().getId())
                .projectName(report.getProject().getName())
                .generatedById(report.getGeneratedBy().getId())
                .generatedByName(report.getGeneratedBy().getFirstName() + " " + report.getGeneratedBy().getLastName())
                .title(report.getTitle())
                .type(report.getType())
                .promptSummary(report.getPromptSummary())
                .content(report.getContent())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }
}
