package com.pricepulse.auth.security;

import com.pricepulse.auth.dto.AuthResponse;
import com.pricepulse.auth.dto.LoginRequest;
import com.pricepulse.auth.entity.User;
import com.pricepulse.auth.repository.UserRepository;
import com.pricepulse.auth.service.AuthService;
import com.pricepulse.auth.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Tests for logout token invalidation (Req 15.1).
 * Exercises the real JwtAuthFilter path (not a mocked JwtService).
 */
@WebMvcTest(controllers = {com.pricepulse.auth.controller.AuthController.class})
@Import({com.pricepulse.auth.security.JwtAuthFilter.class})
class LogoutTokenInvalidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService; // Will be the primary bean from the test configuration

    @MockBean
    private AuthService authService;

    @MockBean
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("user@example.com");
        testUser.setPasswordHash("hash"); // Not used in these tests
        testUser.setTokenInvalidatedAt(null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(authService.saveUser(any(User.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void logoutInvalidatesToken() throws Exception {
        // Generate a token for the test user
        String validToken = jwtService.generateToken(testUser.getId());

        // 1. Token works before logout
        mockMvc.perform(get("/api/auth/me")
                        .header("Cookie", "jwt=" + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.email").value(testUser.getEmail()));

        // 2. Perform logout (this will set tokenInvalidatedAt on the user and save it)
        mockMvc.perform(post("/api/auth/logout")
                        .header("Cookie", "jwt=" + validToken))
                .andExpect(status().isOk());

        // 3. Same token should now be rejected (401)
        mockMvc.perform(get("/api/auth/me")
                        .header("Cookie", "jwt=" + validToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void freshLoginAfterLogoutWorks() throws Exception {
        // Generate a token for the test user (we'll use it to log out)
        String validToken = jwtService.generateToken(testUser.getId());

        // Perform logout to set tokenInvalidatedAt
        mockMvc.perform(post("/api/auth/logout")
                        .header("Cookie", "jwt=" + validToken))
                .andExpect(status().isOk());

        // After logout, a fresh login should work
        LoginRequest loginRequest = new LoginRequest(testUser.getEmail(), "any-password");

        // Mock the authService.login to return our testUser (ignoring password)
        when(authService.login(any(LoginRequest.class), anyString())).thenReturn(testUser);

        // Perform loginRequest loginRequest = new LoginRequest(testUser.getEmail(), "any-password");

        // Mock the authService.login to return our testUser (ignoring password)
        when(authService.login(any(LoginRequest.class), anyString())).thenReturn(testUser);

        // Perform login to get a new token
        String setCookieHeader = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"any-password\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("Set-Cookie");

        // Extract the new token from the Set-Cookie header
        String newToken = extractJwtFromCookie(setCookieHeader);

        // New token should work on a protected endpoint
        mockMvc.perform(get("/api/auth/me")
                        .header("Cookie", "jwt=" + newToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.email").value(testUser.getEmail()));
    }

    private String extractJwtFromCookie(String setCookieHeader) {
        // The Set-Cookie header is like: "jwt=tokenValue; HttpOnly; SameSite=Strict; Path=/api; MaxAge=86400"
        if (setCookieHeader == null) {
            return null;
        }
        // Extract the value between "jwt=" and the next ";"
        int start = setCookieHeader.indexOf("jwt=");
        if (start == -1) {
            return null;
        }
        start += 4; // length of "jwt="
        int end = setCookieHeader.indexOf(';', start);
        if (end == -1) {
            end = setCookieHeader.length();
        }
        return setCookieHeader.substring(start, end);
    }

    /**
     * Test configuration to provide a JwtService with a sufficiently long secret.
     * The secret must be at least 32 characters (256 bits) for HS256.
     */
    @Configuration
    static class JwtServiceTestConfig {
        @Bean
        @Primary
        public JwtService jwtService() {
            // 32-character secret (exactly 256 bits)
            return new JwtService("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        }
    }
}