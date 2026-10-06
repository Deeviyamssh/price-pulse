package com.pricepulse.common.exception;

/**
 * Thrown when business-level validation fails that is not covered by Bean Validation
 * (e.g. a cross-field constraint or a rule that requires a database lookup).
 * Mapped to HTTP 400 by GlobalExceptionHandler.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
