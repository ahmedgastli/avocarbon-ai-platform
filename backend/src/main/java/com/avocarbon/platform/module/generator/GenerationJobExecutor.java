package com.avocarbon.platform.module.generator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

/**
 * Async execution component for the Angular generation pipeline.
 *
 * Extracted into its own Spring-managed bean so that {@code @Async}
 * works correctly via proxy invocation (calling an async method from
 * within the same bean would bypass the proxy and run synchronously).
 *
 * Each invocation:
 *   PENDING → RUNNING → SUCCESS | FAILED
 *
 * Each status transition is saved in its own transaction so that the
 * client polling {@code GET /api/generations/{id}} sees live updates.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GenerationJobExecutor {

    private final GenerationJobRepository  jobRepository;
    private final OpenApiParser            openApiParser;
    private final PromptBuilder            promptBuilder;
    private final McpClient                mcpClient;
    private final AngularProjectGenerator  angularProjectGenerator;
    private final ZipService               zipService;
    private final FileStorageService       fileStorageService;

    /**
     * Execute the full Angular generation pipeline for the given job ID.
     * Runs in the {@code generatorExecutor} thread pool.
     */
    @Async("generatorExecutor")
    @Transactional
    public void executeAsync(Long jobId) {
        GenerationJob job = null;
        for (int i = 0; i < 5; i++) {
            job = jobRepository.findById(jobId).orElse(null);
            if (job != null) break;
            try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        if (job == null) {
            log.error("Generation job not found: " + jobId);
            return;
        }

        try {
            // ── Step 1: RUNNING ─────────────────────────────────────────────
            job.setStatus(GenerationStatus.RUNNING);
            job.setStartedAt(Instant.now());
            jobRepository.save(job);
            log.info("[Job {}] Status → RUNNING", jobId);

            // ── Step 2: Parse OpenAPI ────────────────────────────────────────
            ParsedOpenApiSpec spec = openApiParser.parse(
                    job.getOpenApiContent(), job.getOpenApiFormat());
            log.info("[Job {}] Parsed OpenAPI: '{}', {} resources", jobId, spec.getTitle(), spec.getResources().size());

            // ── Step 3: Build LLM prompt ─────────────────────────────────────
            String prompt = promptBuilder.buildPrompt(spec, job.getType());

            // ── Step 4: Generate via MCP ──────────────────────────────────────
            Map<String, String> llmFiles = mcpClient.generateAngularProject(prompt, spec, job.getType());
            log.info("[Job {}] MCP returned {} application files", jobId, llmFiles.size());

            // ── Step 5: Assemble project (add boilerplate) ───────────────────
            Map<String, String> projectFiles = angularProjectGenerator.assemble(llmFiles, spec, job.getType());
            log.info("[Job {}] Project assembled: {} files total", jobId, projectFiles.size());

            // ── Step 6: ZIP ───────────────────────────────────────────────────
            byte[] zipBytes = zipService.zip(projectFiles);

            // ── Step 7: Persist on disk ───────────────────────────────────────
            String outputPath = fileStorageService.store(jobId, zipBytes);

            // ── Step 8: SUCCESS ───────────────────────────────────────────────
            job.setStatus(GenerationStatus.SUCCESS);
            job.setOutputPath(outputPath);
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);
            log.info("[Job {}] Status → SUCCESS  ({} files, {} bytes ZIP)", jobId, projectFiles.size(), zipBytes.length);

        } catch (Exception e) {
            log.error("[Job {}] Generation FAILED: {}", jobId, e.getMessage(), e);
            String msg = e.getMessage() != null
                    ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 2000))
                    : "Unknown error";
            job.setStatus(GenerationStatus.FAILED);
            job.setErrorMessage(msg);
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);
        }
    }
}
