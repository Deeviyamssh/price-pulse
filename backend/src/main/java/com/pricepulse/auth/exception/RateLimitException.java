package com.pricepulse.auth.exception;

/**
 * Thrown when a client IP has exceeded the login failure threshold and is currently
 * in the 15-minute block window (Req 2.5).
 * Mapped to HTTP 429 by GlobalExceptionHandler.
 */
public class RateLimitException extends RuntimeException {

    public RateLimitException(String message) {
        super(message);
    }
}
