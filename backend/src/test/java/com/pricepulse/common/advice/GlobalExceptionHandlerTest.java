package com.pricepulse.common.advice;

import com.pricepulse.alert.controller.AlertController;
import com.pricepulse.alert.repository.PriceAlertRepository;
import com.pricepulse.alert.service.AlertService;
import com.pricepulse.auth.controller.AuthController;
import com.pricepulse.auth.repository.UserRepository;

import com.pricepulse.auth.service.AuthService;
import com.pricepulse.auth.service.JwtService;
import com.pricepulse.auth.service.LoginRateLimiter;
import com.pricepulse.common.exception.ResourceNotFoundException;
import com.pricepulse.pricing.controller.PriceRecordController;
import com.pricepulse.pricing.repository.PriceRecordRepository;
import com.pricepulse.pricing.service.PriceCheckerService;
import com.pricepulse.pricing.service.PriceRecordService;

import com.pricepulse.product.controller.ProductController;
import com.pricepulse.product.repository.ProductRepository;
import com.pricepulse.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.pricepulse.auth.entity.User;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        String secret = "test-secret-that-is-at-least-32-characters-long";
        AuthService authService = new AuthService(
                mock(UserRepository.class),
                mock(PasswordEncoder.class),
                new JwtService(secret),
                new LoginRateLimiter());
        com.pricepulse.auth.security.AuthCookieProperties cookieProperties =
                new com.pricepulse.auth.security.AuthCookieProperties(
                        new org.springframework.mock.env.MockEnvironment());

        ProductRepository productRepository = mock(ProductRepository.class);
        org.mockito.Mockito.when(productRepository.findByIdAndUserId(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(java.util.Optional.empty());
        ProductService productService = new ProductService(
                productRepository,
                mock(PriceRecordRepository.class),
                mock(PriceCheckerService.class),
                mock(UserRepository.class),
                mock(PriceAlertRepository.class));
        AlertService alertService = new AlertService(productRepository, mock(PriceAlertRepository.class));
        PriceRecordService priceRecordService = new PriceRecordService(
                productRepository,
                mock(PriceRecordRepository.class),
                mock(PriceCheckerService.class),
                mock(com.pricepulse.alert.service.AlertEvaluator.class));

        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AuthController(authService, new JwtService(secret), cookieProperties),
                        new ProductController(productService),
                        new AlertController(alertService),
                        new PriceRecordController(priceRecordService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void malformedJsonReturnsSanitized400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\","))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Request body must contain valid JSON"));
    }

    @Test
    void missingRequiredFieldsReturns400() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("URL must not be blank")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Display name must not be blank")));
    }

    @Test
    void oversizedEmailReturns400BeforeServiceCall() throws Exception {
        String oversizedEmail = "a".repeat(247) + "@example.com";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + oversizedEmail + "\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Email must not exceed 254 characters")));
    }

    @Test
    void oversizedDisplayNameReturns400BeforeServiceCall() throws Exception {
        String oversizedName = "x".repeat(256);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com/product\",\"displayName\":\"" + oversizedName + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Display name must not exceed 255 characters")));
    }

    @Test
    void nonNumericProductIdReturnsSanitized400() throws Exception {
        mockMvc.perform(get("/api/products/not-a-number").with(authenticatedRequest()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Request parameter has an invalid format"));
    }

    @Test
    void nonNumericAlertIdReturnsSanitized400() throws Exception {
        mockMvc.perform(delete("/api/products/not-a-number/alert").with(authenticatedRequest()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void nonNumericPriceHistoryIdReturnsSanitized400() throws Exception {
        mockMvc.perform(get("/api/products/not-a-number/prices").with(authenticatedRequest()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void zeroAndNegativeProductIdsRemainNotFoundAtServiceLevel() {
        ProductRepository repository = mock(ProductRepository.class);
        org.mockito.Mockito.when(repository.findByIdAndUserId(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.eq(1L)))
                .thenReturn(java.util.Optional.empty());
        ProductService service = new ProductService(
                repository,
                mock(PriceRecordRepository.class),
                mock(PriceCheckerService.class),
                mock(UserRepository.class),
                mock(PriceAlertRepository.class));

        org.junit.jupiter.api.Assertions.assertThrows(
                ResourceNotFoundException.class, () -> service.getProduct(1L, 0L));
        org.junit.jupiter.api.Assertions.assertThrows(
                ResourceNotFoundException.class, () -> service.getProduct(1L, -1L));
    }

    @Test
    void zeroAndNegativePriceHistoryIdsRemainNotFoundAtServiceLevel() {
        ProductRepository repository = mock(ProductRepository.class);
        org.mockito.Mockito.when(repository.findByIdAndUserId(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.eq(1L)))
                .thenReturn(java.util.Optional.empty());
        PriceRecordService service = new PriceRecordService(
                repository,
                mock(PriceRecordRepository.class),
                mock(PriceCheckerService.class),
                mock(com.pricepulse.alert.service.AlertEvaluator.class));

        org.junit.jupiter.api.Assertions.assertThrows(
                ResourceNotFoundException.class, () -> service.getPriceHistory(1L, 0L));
        org.junit.jupiter.api.Assertions.assertThrows(
                ResourceNotFoundException.class, () -> service.getPriceHistory(1L, -1L));
    }

    private RequestPostProcessor authenticatedRequest() {
        User user = new User();
        user.setId(1L);
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        return authentication(authentication);
    }

    @Test
    void oversizedLoginPasswordReturns400BeforeServiceCall() throws Exception {
        String oversizedPassword = "p".repeat(129);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"" + oversizedPassword + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Password must not exceed 128 characters")));
    }
}
