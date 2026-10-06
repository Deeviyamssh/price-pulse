package com.pricepulse.alert.service;

import com.pricepulse.alert.dto.AlertResponse;
import com.pricepulse.alert.dto.SetAlertRequest;
import com.pricepulse.alert.entity.PriceAlert;
import com.pricepulse.alert.repository.PriceAlertRepository;
import com.pricepulse.auth.entity.User;
import com.pricepulse.common.exception.ResourceNotFoundException;
import com.pricepulse.product.entity.Product;
import com.pricepulse.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PriceAlertRepository priceAlertRepository;

    private AlertService alertService;
    private Product product;

    @BeforeEach
    void setUp() {
        alertService = new AlertService(productRepository, priceAlertRepository);
        User user = new User();
        user.setId(1L);
        product = new Product();
        product.setId(10L);
        product.setUser(user);
    }

    @Test
    void createsActiveAlertWithNoNotificationTimestamp() {
        givenAlertSave();
        givenOwnedProduct();
        when(priceAlertRepository.findByProductId(10L)).thenReturn(Optional.empty());

        AlertResponse response = alertService.setAlert(1L, 10L,
                new SetAlertRequest(new BigDecimal("80.00")));

        assertThat(response.targetPrice()).isEqualByComparingTo("80.00");
        assertThat(response.isActive()).isTrue();
        assertThat(response.lastNotifiedAt()).isNull();
    }

    @Test
    void inactiveAlertWithNumericallySameTargetRemainsInactive() {
        givenAlertSave();
        givenOwnedProduct();
        PriceAlert alert = existingAlert(new BigDecimal("80.0"), false, Instant.parse("2025-01-01T00:00:00Z"));
        when(priceAlertRepository.findByProductId(10L)).thenReturn(Optional.of(alert));

        AlertResponse response = alertService.setAlert(1L, 10L,
                new SetAlertRequest(new BigDecimal("80.00")));

        assertThat(response.isActive()).isFalse();
        assertThat(response.lastNotifiedAt()).isEqualTo(Instant.parse("2025-01-01T00:00:00Z"));
        assertThat(alert.getTargetPrice()).isEqualByComparingTo("80.00");
    }

    @Test
    void inactiveAlertWithDifferentTargetRearmsAndClearsNotificationTimestamp() {
        givenAlertSave();
        givenOwnedProduct();
        PriceAlert alert = existingAlert(new BigDecimal("80.00"), false, Instant.parse("2025-01-01T00:00:00Z"));
        when(priceAlertRepository.findByProductId(10L)).thenReturn(Optional.of(alert));

        AlertResponse response = alertService.setAlert(1L, 10L,
                new SetAlertRequest(new BigDecimal("75.00")));

        assertThat(response.isActive()).isTrue();
        assertThat(response.lastNotifiedAt()).isNull();
    }

    @Test
    void removeAlertDeactivatesExistingAlert() {
        givenAlertSave();
        givenOwnedProduct();
        PriceAlert alert = existingAlert(new BigDecimal("80.00"), true, null);
        when(priceAlertRepository.findByProductId(10L)).thenReturn(Optional.of(alert));

        AlertResponse response = alertService.removeAlert(1L, 10L);

        assertThat(response.isActive()).isFalse();
        verify(priceAlertRepository).save(alert);
    }

    @Test
    void removeAlertWithoutAlertReturnsNotFound() {
        givenOwnedProduct();
        when(priceAlertRepository.findByProductId(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertService.removeAlert(1L, 10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Price alert not found");
    }

    private void givenAlertSave() {
        when(priceAlertRepository.save(any(PriceAlert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void givenOwnedProduct() {
        when(productRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(product));
    }

    private PriceAlert existingAlert(BigDecimal target, boolean active, Instant lastNotifiedAt) {
        PriceAlert alert = new PriceAlert();
        alert.setId(20L);
        alert.setProduct(product);
        alert.setTargetPrice(target);
        alert.setActive(active);
        alert.setLastNotifiedAt(lastNotifiedAt);
        return alert;
    }
}
