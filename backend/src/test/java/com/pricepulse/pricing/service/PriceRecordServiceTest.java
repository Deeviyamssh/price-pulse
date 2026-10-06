package com.pricepulse.pricing.service;

import com.pricepulse.alert.service.AlertEvaluator;
import com.pricepulse.auth.entity.User;
import com.pricepulse.pricing.entity.CheckStatus;
import com.pricepulse.pricing.entity.PriceRecord;
import com.pricepulse.pricing.repository.PriceRecordRepository;
import com.pricepulse.product.entity.Product;
import com.pricepulse.product.entity.ProductStatus;
import com.pricepulse.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceRecordServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PriceRecordRepository priceRecordRepository;

    @Mock
    private PriceCheckerService priceCheckerService;

    @Mock
    private AlertEvaluator alertEvaluationService;

    @InjectMocks
    private PriceRecordService priceRecordService;

    private User testUser;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        product1 = new Product();
        product1.setId(10L);
        product1.setUser(testUser);
        product1.setUrl("https://example.com/item/1");
        product1.setDisplayName("Product One");
        product1.setStatus(ProductStatus.ACTIVE);

        product2 = new Product();
        product2.setId(20L);
        product2.setUser(testUser);
        product2.setUrl("https://example.com/item/2");
        product2.setDisplayName("Product Two");
        product2.setStatus(ProductStatus.ACTIVE);
    }

    @Test
    @DisplayName("checkAllActiveProducts saves SUCCESS PriceRecord for each active product")
    void checkAllActiveProducts_success() {
        when(productRepository.findByStatusWithUser(ProductStatus.ACTIVE)).thenReturn(List.of(product1, product2));
        when(priceCheckerService.check("https://example.com/item/1"))
                .thenReturn(PriceResult.success(new BigDecimal("99.99"), "USD"));
        when(priceCheckerService.check("https://example.com/item/2"))
                .thenReturn(PriceResult.success(new BigDecimal("149.50"), "USD"));

        priceRecordService.checkAllActiveProducts();

        ArgumentCaptor<PriceRecord> recordCaptor = ArgumentCaptor.forClass(PriceRecord.class);
        verify(priceRecordRepository, times(2)).save(recordCaptor.capture());

        List<PriceRecord> savedRecords = recordCaptor.getAllValues();
        assertThat(savedRecords).hasSize(2);

        assertThat(savedRecords.get(0).getProduct()).isEqualTo(product1);
        assertThat(savedRecords.get(0).getPrice()).isEqualTo(new BigDecimal("99.99"));
        assertThat(savedRecords.get(0).getCheckStatus()).isEqualTo(CheckStatus.SUCCESS);
        assertThat(savedRecords.get(0).getErrorMessage()).isNull();

        assertThat(savedRecords.get(1).getProduct()).isEqualTo(product2);
        assertThat(savedRecords.get(1).getPrice()).isEqualTo(new BigDecimal("149.50"));
        assertThat(savedRecords.get(1).getCheckStatus()).isEqualTo(CheckStatus.SUCCESS);
        assertThat(savedRecords.get(1).getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("checkAllActiveProducts stores FAILED PriceRecord when checker returns FAILED status")
    void checkAllActiveProducts_checkerFailedStatus() {
        when(productRepository.findByStatusWithUser(ProductStatus.ACTIVE)).thenReturn(List.of(product1));
        when(priceCheckerService.check("https://example.com/item/1"))
                .thenReturn(PriceResult.failed("Connection timed out"));

        priceRecordService.checkAllActiveProducts();

        ArgumentCaptor<PriceRecord> recordCaptor = ArgumentCaptor.forClass(PriceRecord.class);
        verify(priceRecordRepository).save(recordCaptor.capture());

        PriceRecord saved = recordCaptor.getValue();
        assertThat(saved.getProduct()).isEqualTo(product1);
        assertThat(saved.getPrice()).isNull();
        assertThat(saved.getCurrency()).isNull();
        assertThat(saved.getCheckStatus()).isEqualTo(CheckStatus.FAILED);
        assertThat(saved.getErrorMessage()).isEqualTo("Connection timed out");
    }

    @Test
    @DisplayName("Requirement 9.5: Per-product exception isolation — one product throwing unhandled exception does not abort remaining products")
    void checkAllActiveProducts_exceptionIsolation() {
        when(productRepository.findByStatusWithUser(ProductStatus.ACTIVE)).thenReturn(List.of(product1, product2));
        when(priceCheckerService.check("https://example.com/item/1"))
                .thenThrow(new RuntimeException("Simulated unexpected network crash"));
        when(priceCheckerService.check("https://example.com/item/2"))
                .thenReturn(PriceResult.success(new BigDecimal("199.99"), "USD"));

        priceRecordService.checkAllActiveProducts();

        ArgumentCaptor<PriceRecord> recordCaptor = ArgumentCaptor.forClass(PriceRecord.class);
        verify(priceRecordRepository, times(2)).save(recordCaptor.capture());

        List<PriceRecord> savedRecords = recordCaptor.getAllValues();
        assertThat(savedRecords).hasSize(2);

        // First product captured exception as FAILED record
        assertThat(savedRecords.get(0).getProduct()).isEqualTo(product1);
        assertThat(savedRecords.get(0).getCheckStatus()).isEqualTo(CheckStatus.FAILED);
        assertThat(savedRecords.get(0).getErrorMessage()).isEqualTo("Simulated unexpected network crash");

        // Second product was still processed and saved as SUCCESS
        assertThat(savedRecords.get(1).getProduct()).isEqualTo(product2);
        assertThat(savedRecords.get(1).getCheckStatus()).isEqualTo(CheckStatus.SUCCESS);
        assertThat(savedRecords.get(1).getPrice()).isEqualTo(new BigDecimal("199.99"));
    }

    @Test
    @DisplayName("checkAllActiveProducts does nothing when no active products exist")
    void checkAllActiveProducts_emptyList() {
        when(productRepository.findByStatusWithUser(ProductStatus.ACTIVE)).thenReturn(List.of());

        priceRecordService.checkAllActiveProducts();

        verify(priceCheckerService, never()).check(any());
        verify(priceRecordRepository, never()).save(any());
    }
}
