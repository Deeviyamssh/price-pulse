package com.pricepulse.auth.controller;

import com.pricepulse.auth.dto.AuthResponse;
import com.pricepulse.auth.dto.LoginRequest;
import com.pricepulse.auth.dto.RegisterRequest;
import com.pricepulse.auth.entity.User;
import com.pricepulse.auth.security.AuthCookieProperties;
import com.pricepulse.auth.service.AuthService;
import com.pricepulse.auth.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Handles registration, login, and logout.
 *
 * <p>The JWT is placed in an {@code HttpOnly; Secure; SameSite=Strict} cookie so that
 * JavaScript cannot read it (XSS protection) and it is not sent to cross-origin requests
 * (CSRF protection via SameSite). The response body on login carries only non-sensitive
 * user metadata (userId, email) so the frontend can display the user's identity without
 * needing to decode the token.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST /api/auth/register} — create a new account (Req 1.x)</li>
 *   <li>{@code POST /api/auth/login}    — authenticate and receive a JWT cookie (Req 2.x)</li>
 *   <li>{@code POST /api/auth/logout}   — clear the JWT cookie (Req 15.x)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    static final String JWT_COOKIE_NAME = "jwt";
    private static final long COOKIE_MAX_AGE_SECONDS = 86_400; // 24 hours — matches JWT expiry

    private final AuthService authService;
    private final JwtService jwtService;
    private final AuthCookieProperties authCookieProperties;

    public AuthController(AuthService authService,
                          JwtService jwtService,
                          AuthCookieProperties authCookieProperties) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.authCookieProperties = authCookieProperties;
    }

    // ── Registration ────────────────────────────────────────────────────────

    /**
     * Create a new user account.
     *
     * <p>Bean Validation on {@link RegisterRequest} enforces email format and password length.
     * The service performs the duplicate-email check.
     *
     * @return 201 Created with the new user's id and email
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest req) {

        User user = authService.register(req);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new AuthResponse(user.getId(), user.getEmail()));
    }

    // ── Login ────────────────────────────────────────────────────────────────

    /**
     * Authenticate with email + password.
     *
     * <p>On success, a signed JWT is issued in an {@code HttpOnly} cookie.
     * On failure (bad credentials or rate-limited), exceptions are thrown and handled
     * by {@code GlobalExceptionHandler} → 401 / 429.
     *
     * @return 200 OK with {@link AuthResponse} body and {@code Set-Cookie} header
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest req,
            HttpServletRequest httpRequest) {

        String clientIp = resolveClientIp(httpRequest);
        User user = authService.login(req, clientIp);

        String token = jwtService.generateToken(user.getId());
        ResponseCookie cookie = buildJwtCookie(token, COOKIE_MAX_AGE_SECONDS);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthResponse(user.getId(), user.getEmail()));
    }

    // ── Logout ───────────────────────────────────────────────────────────────

    /**
     * End the current session by clearing the JWT cookie.
     *
     * <p>The endpoint is protected (requires authentication) so that unauthenticated
     * requests receive 401 from the security filter chain before reaching this method
     * (Req 15.3). The actual session invalidation is done by removing the cookie on the
     * client side — since JWTs are stateless there is no server-side session to destroy
     * (Req 15.1 is fulfilled by the cookie removal making the stored token unusable from
     * the browser, and Req 15.4 is fulfilled by the cookie being gone on subsequent requests).
     *
     * @return 200 OK with a cleared (max-age=0) jwt cookie
     */
    /**
     * Returns the user authenticated by the JWT cookie.
     * Anonymous requests are rejected by Spring Security before this method runs.
     */
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> currentUser(User user) {
        return ResponseEntity.ok(new AuthResponse(user.getId(), user.getEmail()));
    }

    @Transactional
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(User user) {
        user.setTokenInvalidatedAt(Instant.now());
        authService.saveUser(user);
        ResponseCookie clearedCookie = buildJwtCookie("", 0);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearedCookie.toString())
                .build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Build the JWT {@code ResponseCookie} with the required security attributes (Req 2.1).
     *
     * @param value   the JWT string (empty string to clear the cookie)
     * @param maxAge  lifetime in seconds (0 to expire immediately)
     */
    private ResponseCookie buildJwtCookie(String value, long maxAge) {
        return ResponseCookie.from(JWT_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(authCookieProperties.isCookieSecure())
                .sameSite("Strict")
                .path("/api")
                .maxAge(maxAge)
                .build();
    }

    /**
     * Resolve the real client IP, honouring the {@code X-Forwarded-For} header when the
     * application sits behind a reverse proxy or load balancer.
     *
     * <p>Only the first address in a multi-value {@code X-Forwarded-For} header is used
     * (the leftmost is the originating client; later entries are added by each proxy).
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // "X-Forwarded-For: client, proxy1, proxy2" — take the first entry
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
