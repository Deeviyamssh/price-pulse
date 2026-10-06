package com.pricepulse.common.dto;

import java.time.Instant;

/**
 * Standard error envelope returned by GlobalExceptionHandler for all error responses.
 * Stack traces, class names, and raw SQL text are never included here — they are
 * logged server-side only (Req 14.2).
 *
 * <pre>
 * {
 *   "timestamp": "2024-01-15T14:30:00Z",
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "Display name must not be blank"
 * }
 * </pre>
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message
) {}
