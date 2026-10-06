package com.pricepulse.auth.security;

import com.pricepulse.auth.repository.UserRepository;
import com.pricepulse.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

/**
 * Reads the {@code jwt} HttpOnly cookie from every incoming request.
 * <ul>
 *   <li>Cookie absent → pass through anonymously (Spring Security returns 401
 *       for protected endpoints because no Authentication is set).</li>
 *   <li>Cookie present but invalid/expired → pass through anonymously.</li>
 *   <li>Cookie present and valid → extract userId, load User, set
 *       {@link UsernamePasswordAuthenticationToken} in the
 *       {@link SecurityContextHolder}.</li>
 * </ul>
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        // 1. Extract the "jwt" cookie — absent means anonymous request
        String token = extractJwtCookie(request).orElse(null);
        if (token == null) {
            chain.doFilter(request, response);
            return;
        }

        // 2. Validate token — invalid or expired means anonymous request
        if (!jwtService.isValid(token)) {
            chain.doFilter(request, response);
            return;
        }

        // 3. Token is valid: extract userId and load the User
        Long userId = jwtService.extractUserId(token);
        UserDetails user = userRepository.findById(userId).orElse(null);

        // Guard against a valid token whose user was deleted after issuance
        if (user == null) {
            chain.doFilter(request, response);
            return;
        }

        if (user instanceof com.pricepulse.auth.entity.User pricePulseUser
                && pricePulseUser.getTokenInvalidatedAt() != null
                && jwtService.extractIssuedAt(token).isBefore(pricePulseUser.getTokenInvalidatedAt())) {
            chain.doFilter(request, response);
            return;
        }

        // 4. Build authentication token and store in SecurityContextHolder
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);

        chain.doFilter(request, response);
    }

    // ── Helper ──────────────────────────────────────────────────────────────

    private Optional<String> extractJwtCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(c -> "jwt".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
