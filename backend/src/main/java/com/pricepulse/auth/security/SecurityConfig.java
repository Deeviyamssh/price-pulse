package com.pricepulse.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Map;

/**
 * Central Spring Security configuration.
 *
 * <ul>
 *   <li>CSRF disabled — the JWT is stored in a {@code SameSite=Strict} HttpOnly cookie,
 *       which already prevents cross-site request forgery without a CSRF token.</li>
 *   <li>Sessions are STATELESS — all authentication state lives in the JWT.</li>
 *   <li>{@code /api/auth/**} and {@code /actuator/health} are public; everything else
 *       requires a valid JWT.</li>
 *   <li>CORS allows the React dev server at {@code http://localhost:3000} with
 *       credentials so the browser sends the HttpOnly cookie on cross-origin requests.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF not needed: SameSite=Strict cookie blocks cross-origin requests
                .csrf(AbstractHttpConfigurer::disable)

                // CORS must be configured before authentication so pre-flight OPTIONS
                // requests are handled without requiring a JWT
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // No server-side session — every request is authenticated via JWT
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Return 401 when a request has no valid authentication. This applies only
                // to the security filter chain; controller-level ownership checks retain
                // their deliberate 404 behavior.
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint()))

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/actuator/health").permitAll()
                        .requestMatchers("/api/auth/me", "/api/auth/logout").authenticated()
                        .anyRequest().authenticated()
                )

                // JWT filter runs before the standard username/password filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) ->
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    }

    /**
     * BCrypt with cost factor 12 (~300 ms per hash on commodity hardware).
     * High enough to deter brute-force attacks; imperceptible to end users on login.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Expose the {@link AuthenticationManager} so that {@code AuthService}
     * can call {@code authenticationManager.authenticate()} during login.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    // ── CORS ────────────────────────────────────────────────────────────────

    /**
     * Allow the React dev server to call the API with credentials (the HttpOnly cookie).
     *
     * <p>In production, replace {@code http://localhost:3000} with the real domain.</p>
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Allowed origin — must be explicit (not "*") when credentials are enabled
        config.setAllowedOrigins(List.of("http://localhost:3000"));

        // All methods used by the API
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Only Content-Type is sent by the API client
        config.setAllowedHeaders(List.of("Content-Type"));

        // Required for the browser to include the HttpOnly JWT cookie on cross-origin calls
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
