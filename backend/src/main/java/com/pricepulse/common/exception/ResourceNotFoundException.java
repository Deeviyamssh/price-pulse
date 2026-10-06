package com.pricepulse.common.exception;

/**
 * Thrown when a requested resource does not exist or the requesting user does not own it.
 * Mapped to HTTP 404 by GlobalExceptionHandler.
 * Returning 404 (not 403) on ownership violations is intentional — it reveals nothing
 * about whether the resource exists for other users.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
