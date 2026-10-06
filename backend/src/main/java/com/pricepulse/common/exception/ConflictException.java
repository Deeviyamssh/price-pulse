package com.pricepulse.common.exception;

/**
 * Thrown when a request conflicts with the current state of a resource — for example,
 * registering a duplicate email or adding a URL the user is already monitoring.
 * Mapped to HTTP 409 by GlobalExceptionHandler.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
