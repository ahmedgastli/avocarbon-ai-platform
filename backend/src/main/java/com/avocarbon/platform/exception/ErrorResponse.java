package com.avocarbon.platform.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

/**
 * Standard error response DTO.
 *
 * Used to return error information to clients via REST API.
 * Provides consistent error response format across the application.
 *
 * @since 1.0.0
 */
@Getter
@Setter
@AllArgsConstructor
@Builder
public class ErrorResponse {

    private int status;
    private String message;
    private String path;
    private Instant timestamp;

}
