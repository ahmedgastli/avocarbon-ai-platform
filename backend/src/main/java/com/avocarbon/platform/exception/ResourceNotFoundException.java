package com.avocarbon.platform.exception;

/**
 * Exception thrown when a requested resource is not found.
 *
 * This exception is typically caught by the global exception handler
 * and converted to a 404 NOT_FOUND HTTP response.
 *
 * @since 1.0.0
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructs a new ResourceNotFoundException with the specified detail message.
     *
     * @param message the detail message
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new ResourceNotFoundException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
