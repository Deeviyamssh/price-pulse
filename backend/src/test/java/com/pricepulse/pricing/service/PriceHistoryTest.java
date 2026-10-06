package com.pricepulse.pricing.service;

import com.pricepulse.auth.entity.User;
import com.pricepulse.common.exception.ResourceNotFoundException;
import com.pricepulse.pricing.dto.PriceRecordResponse;
import com.pricepulse.pricing.entity.CheckStatus;
import com.pricepulse.pricing.entity.PriceRecord;
import com.pricepulse.pricing.repository.PriceRecordRepository;
import com.pricepulse.product.entity.Product;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceHistoryTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PriceRecordRepository priceRecordRepository;

    @Mock
    private PriceCheckerService priceCheckerService;

    @Mock
    private com.pricepulse.alert.service.AlertEvaluator alertEvaluator;

    private PriceRecordService priceRecordService;
    private Product product;

    @BeforeEach
    void setUp() {
        priceRecordService = new PriceRecordService(
                productRepository, priceRecordRepository, priceCheckerService, alertEvaluator);
        User user = new User();
        user.setId(1L);
        product = new Product();
        product.setId(2L);
        product.setUser(user);
    }

    @Test
    void returnsMappedHistoryInRepositoryOrder() {
        when(productRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(product));
        PriceRecord success = PriceRecord.from(product,
                PriceResult.success(new BigDecimal("75.00"), "USD"));
        success.setId(1L);
        success.setCheckedAt(Instant.parse("2025-01-01T00:00:00Z"));
        PriceRecord failed = PriceRecord.failed(product, "Connection timed out");
        failed.setId(2L);
        failed.setCheckedAt(Instant.parse("2025-01-02T00:00:00Z"));
        when(priceRecordRepository.findByProductIdOrderByCheckedAtAscIdAsc(2L))
                .thenReturn(List.of(success, failed));

        List<PriceRecordResponse> history = priceRecordService.getPriceHistory(1L, 2L);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).checkStatus()).isEqualTo(CheckStatus.SUCCESS);
        assertThat(history.get(0).price()).isEqualByComparingTo("75.00");
        assertThat(history.get(1).checkStatus()).isEqualTo(CheckStatus.FAILED);
        assertThat(history.get(1).price()).isNull();
        assertThat(history.get(1).errorMessage()).isEqualTo("Connection timed out");
    }

    @Test
    void rejectsMissingOrCrossUserProductBeforeLoadingHistory() {
        when(productRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> priceRecordService.getPriceHistory(1L, 2L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product not found");
        verifyNoInteractions(priceRecordRepository);
    }
}
