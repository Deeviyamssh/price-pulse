package com.pricepulse.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/auth/login.
 * Validation is intentionally minimal here — the service handles credential mismatch
 * and returns a generic 401 message that does not disclose which field was wrong (Req 2.2, 2.3).
 */
public record LoginRequest(

        @NotBlank(message = "Email must not be blank")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        String email,

        @NotBlank(message = "Password must not be blank")
        @Size(max = 128, message = "Password must not exceed 128 characters")
        String password

) {}
