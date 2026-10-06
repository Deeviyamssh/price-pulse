package com.pricepulse.product.service;

import com.pricepulse.alert.entity.PriceAlert;
import com.pricepulse.alert.repository.PriceAlertRepository;
import com.pricepulse.auth.entity.User;
import com.pricepulse.pricing.repository.PriceRecordRepository;
import com.pricepulse.pricing.service.PriceCheckerService;
import com.pricepulse.product.dto.ProductResponse;
import com.pricepulse.product.entity.Product;
import com.pricepulse.product.entity.ProductStatus;
import com.pricepulse.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceAlertSummaryTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PriceRecordRepository priceRecordRepository;

    @Mock
    private PriceCheckerService priceCheckerService;

    @Mock
    private com.pricepulse.auth.repository.UserRepository userRepository;

    @Mock
    private PriceAlertRepository priceAlertRepository;

    private ProductService productService;
    private Product product;

    @BeforeEach
    void setUp() {
        productService = new ProductService(
                productRepository,
                priceRecordRepository,
                priceCheckerService,
                userRepository,
                priceAlertRepository);

        User user = new User();
        user.setId(1L);
        product = new Product();
        product.setId(2L);
        product.setUser(user);
        product.setUrl("https://example.com/item/2");
        product.setDisplayName("Product Two");
        product.setStatus(ProductStatus.ACTIVE);
        product.setCreatedAt(Instant.parse("2025-01-01T00:00:00Z"));

        when(productRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(product));

        when(priceRecordRepository.findTop2ByProductIdAndCheckStatusOrderByCheckedAtDescIdDesc(
                2L, com.pricepulse.pricing.entity.CheckStatus.SUCCESS)).thenReturn(List.of());
        when(priceRecordRepository.findMinPriceByProductIdAndCheckStatus(
                2L, com.pricepulse.pricing.entity.CheckStatus.SUCCESS)).thenReturn(Optional.empty());
        when(priceRecordRepository.findTopByProductIdOrderByCheckedAtDescIdDesc(2L))
                .thenReturn(Optional.empty());
    }

    @Test
    void productSummaryIncludesPersistedActiveAlert() {
        PriceAlert alert = new PriceAlert();
        alert.setId(1L);
        alert.setProduct(product);
        alert.setTargetPrice(new BigDecimal("75.00"));
        alert.setActive(true);
        alert.setLastNotifiedAt(null);
        when(priceAlertRepository.findByProductId(2L)).thenReturn(Optional.of(alert));

        ProductResponse response = productService.getProduct(1L, 2L);

        assertThat(response.targetPrice()).isEqualByComparingTo("75.00");
        assertThat(response.alertActive()).isTrue();
    }
}
