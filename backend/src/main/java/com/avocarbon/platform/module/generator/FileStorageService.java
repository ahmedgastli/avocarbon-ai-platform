package com.avocarbon.platform.module.generator;

import com.avocarbon.platform.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Stream;

/**
 * Persists and retrieves generated ZIP archives on the local filesystem.
 *
 * Each job gets its own directory under the configured storage root:
 *   {app.storage.path}/{jobId}/project.zip
 *
 * Configure the root in application.properties:
 *   app.storage.path=storage/generations
 */
@Component
@Slf4j
public class FileStorageService {

    private final Path storageRoot;

    public FileStorageService(
            @Value("${app.storage.path:storage/generations}") String storagePath) {
        this.storageRoot = Paths.get(storagePath).toAbsolutePath();
        log.info("FileStorageService initialised. Storage root: {}", storageRoot);
    }

    /**
     * Write ZIP bytes to disk under {storageRoot}/{jobId}/project.zip.
     *
     * @return the absolute path string of the created file
     */
    public String store(Long jobId, byte[] zipContent) {
        try {
            Path jobDir  = storageRoot.resolve(String.valueOf(jobId));
            Files.createDirectories(jobDir);
            Path zipPath = jobDir.resolve("project.zip");
            Files.write(zipPath, zipContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("Stored generation ZIP for job {}: {} bytes at {}", jobId, zipContent.length, zipPath);
            return zipPath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to store generation output for job " + jobId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Load the ZIP bytes for a previously stored job.
     *
     * @throws ResourceNotFoundException if the file does not exist
     */
    public byte[] load(String outputPath) {
        try {
            Path path = Paths.get(outputPath);
            if (!Files.exists(path)) {
                throw new ResourceNotFoundException("Generated file not found at: " + outputPath);
            }
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read generation output: " + e.getMessage(), e);
        }
    }

    /**
     * Delete the ZIP file and its parent job directory if empty.
     * Safe to call even if the path is null or the file no longer exists.
     */
    public void delete(String outputPath) {
        if (outputPath == null || outputPath.isBlank()) return;
        try {
            Path zipPath = Paths.get(outputPath);
            Files.deleteIfExists(zipPath);
            Path jobDir = zipPath.getParent();
            if (jobDir != null && Files.isDirectory(jobDir)) {
                try (Stream<Path> stream = Files.list(jobDir)) {
                    if (stream.findAny().isEmpty()) {
                        Files.deleteIfExists(jobDir);
                    }
                }
            }
            log.info("Deleted generation output: {}", outputPath);
        } catch (IOException e) {
            log.warn("Failed to clean up output file '{}': {}", outputPath, e.getMessage());
        }
    }
}
