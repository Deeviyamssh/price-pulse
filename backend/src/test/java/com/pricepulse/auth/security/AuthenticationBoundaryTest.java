package com.pricepulse.auth.security;

import com.pricepulse.alert.controller.AlertController;
import com.pricepulse.auth.service.JwtService;
import com.pricepulse.alert.service.AlertService;
import com.pricepulse.auth.entity.User;
import com.pricepulse.auth.repository.UserRepository;
import com.pricepulse.common.advice.GlobalExceptionHandler;
import com.pricepulse.pricing.controller.PriceRecordController;
import com.pricepulse.pricing.service.PriceRecordService;
import com.pricepulse.product.controller.ProductController;
import com.pricepulse.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;


import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.servlet.http.Cookie;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        ProductController.class,
        AlertController.class,
        PriceRecordController.class
})
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
class AuthenticationBoundaryTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private ProductService productService;

    @MockBean
    private AlertService alertService;

    @MockBean
    private PriceRecordService priceRecordService;

    @BeforeEach
    void setUp() {
        when(jwtService.isValid("valid-token")).thenReturn(true);
        when(jwtService.extractUserId("valid-token")).thenReturn(1L);
        when(jwtService.extractIssuedAt("valid-token")).thenReturn(Instant.now());
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    }

    @ParameterizedTest(name = "{index}: {0} with {1} cookie")
    @MethodSource("protectedRouteAndCookieStates")
    void authenticationBoundaryIsConsistentAcrossRouteFamilies(
            String routeName,
            String path,
            String method,
            String cookieState,
            String cookieValue,
            int expectedStatus) throws Exception {
        if (cookieValue != null) {
            boolean valid = "valid-token".equals(cookieValue);
            when(jwtService.isValid(cookieValue)).thenReturn(valid);
        }

        var builder = "DELETE".equals(method)
                ? delete(path)
                : get(path);
        if (cookieValue != null) {
            builder.cookie(new Cookie("jwt", cookieValue));
        }

        mockMvc.perform(builder)
                .andExpect(status().is(expectedStatus));
    }

    static Stream<Arguments> protectedRouteAndCookieStates() {
        return Stream.of(
                Arguments.of("products", "/api/products", "GET", "no", null, 401),
                Arguments.of("alerts", "/api/products/1/alert", "DELETE", "no", null, 401),
                Arguments.of("price history", "/api/products/1/prices", "GET", "no", null, 401),
                Arguments.of("products", "/api/products", "GET", "malformed", "malformed-token", 401),
                Arguments.of("alerts", "/api/products/1/alert", "DELETE", "malformed", "malformed-token", 401),
                Arguments.of("price history", "/api/products/1/prices", "GET", "malformed", "malformed-token", 401),
                Arguments.of("products", "/api/products", "GET", "expired", "expired-token", 401),
                Arguments.of("alerts", "/api/products/1/alert", "DELETE", "expired", "expired-token", 401),
                Arguments.of("price history", "/api/products/1/prices", "GET", "expired", "expired-token", 401),
                Arguments.of("products", "/api/products", "GET", "valid", "valid-token", 200),
                Arguments.of("alerts", "/api/products/1/alert", "DELETE", "valid", "valid-token", 200),
                Arguments.of("price history", "/api/products/1/prices", "GET", "valid", "valid-token", 200)
        );
    }
}
