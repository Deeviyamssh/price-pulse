package com.pricepulse.auth.exception;

/**
 * Thrown when login fails due to unrecognised email or wrong password.
 * The message is always the generic "Invalid credentials" — the caller must never
 * disclose which of the two fields caused the failure (Req 2.2, 2.3).
 * Mapped to HTTP 401 by GlobalExceptionHandler.
 */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }
}
