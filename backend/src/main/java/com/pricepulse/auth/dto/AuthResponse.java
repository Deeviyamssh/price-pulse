package com.pricepulse.auth.dto;

/**
 * Response body returned on successful login (Req 2.1).
 * The JWT itself travels in the Set-Cookie header, not here.
 */
public record AuthResponse(
        Long userId,
        String email
) {}
