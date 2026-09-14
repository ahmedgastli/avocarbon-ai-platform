package com.avocarbon.platform.module.generator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Converts a file map (path → content) into a ZIP archive in memory.
 *
 * All file contents are encoded as UTF-8.
 */
@Component
@Slf4j
public class ZipService {

    /**
     * Pack all provided files into a single in-memory ZIP archive.
     *
     * @param files map of relative file path → file text content
     * @return raw ZIP bytes
     * @throws RuntimeException if I/O error occurs
     */
    public byte[] zip(Map<String, String> files) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(bos)) {

            for (Map.Entry<String, String> entry : files.entrySet()) {
                byte[] bytes = entry.getValue().getBytes(StandardCharsets.UTF_8);
                ZipEntry zipEntry = new ZipEntry(entry.getKey());
                zipEntry.setSize(bytes.length);
                zos.putNextEntry(zipEntry);
                zos.write(bytes);
                zos.closeEntry();
            }

            zos.finish();
            byte[] result = bos.toByteArray();
            log.debug("ZIP created: {} files, {} bytes total", files.size(), result.length);
            return result;

        } catch (IOException e) {
            throw new RuntimeException("Failed to create ZIP archive: " + e.getMessage(), e);
        }
    }
}
