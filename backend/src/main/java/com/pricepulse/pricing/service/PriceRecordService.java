package com.pricepulse.pricing.service;

import com.pricepulse.alert.service.AlertEvaluator;
import com.pricepulse.pricing.dto.PriceRecordResponse;
import com.pricepulse.pricing.entity.CheckStatus;
import com.pricepulse.pricing.entity.PriceRecord;
import com.pricepulse.pricing.repository.PriceRecordRepository;
import com.pricepulse.product.entity.Product;
import com.pricepulse.product.entity.ProductStatus;
import com.pricepulse.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service managing price record persistence and scheduled batch price checks.
 */
@Service
public class PriceRecordService {

    private static final Logger log = LoggerFactory.getLogger(PriceRecordService.class);

    private final ProductRepository productRepository;
    private final PriceRecordRepository priceRecordRepository;
    private final PriceCheckerService priceCheckerService;
    private final AlertEvaluator alertEvaluationService;

    public PriceRecordService(
            ProductRepository productRepository,
            PriceRecordRepository priceRecordRepository,
            PriceCheckerService priceCheckerService,
            AlertEvaluator alertEvaluationService) {
        this.productRepository = productRepository;
        this.priceRecordRepository = priceRecordRepository;
        this.priceCheckerService = priceCheckerService;
        this.alertEvaluationService = alertEvaluationService;
    }

    /**
     * Returns the complete price history for a product owned by the user.
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<PriceRecordResponse> getPriceHistory(Long userId, Long productId) {
        productRepository.findByIdAndUserId(productId, userId)
                .orElseThrow(() -> new com.pricepulse.common.exception.ResourceNotFoundException(
                        "Product not found"));

        return priceRecordRepository.findByProductIdOrderByCheckedAtAscIdAsc(productId)
                .stream()
                .map(record -> new PriceRecordResponse(
                        record.getId(),
                        record.getPrice(),
                        record.getCurrency(),
                        record.getCheckedAt(),
                        record.getCheckStatus(),
                        record.getErrorMessage()))
                .toList();
    }

    /**
     * Iterates over all ACTIVE products in the system, checks their current price
     * using {@link PriceCheckerService}, and records the outcome (SUCCESS or FAILED)
     * as a new {@link PriceRecord}.
     *
     * <p><strong>Per-product exception isolation (Requirement 9.5):</strong> Each product
     * is processed inside its own {@code try-catch} block. If a check throws an unhandled
     * exception or error, a FAILED {@link PriceRecord} is saved with the error message and
     * the loop continues to the next product without interruption.</p>
     */
    public void checkAllActiveProducts() {
        List<Product> activeProducts = productRepository.findByStatusWithUser(ProductStatus.ACTIVE);
        log.info("Starting price check run for {} active products", activeProducts.size());

        for (Product product : activeProducts) {
            try {
                PriceResult result = priceCheckerService.check(product.getUrl());
                PriceRecord record = PriceRecord.from(product, result);
                priceRecordRepository.save(record);
                log.debug("Recorded price check for product id={} with status={}",
                        product.getId(), result.status());

                if (result.status() == CheckStatus.SUCCESS) {
                    try {
                        alertEvaluationService.evaluate(product, result.price());
                    } catch (Exception alertException) {
                        // Alert failures must not recategorize a successful price check as FAILED.
                        log.error("Alert evaluation failed for product id={}: {}",
                                product.getId(), alertException.getMessage(), alertException);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to check product id={}: {}", product.getId(), e.getMessage(), e);
                try {
                    PriceRecord failedRecord = PriceRecord.failed(product,
                            e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                    priceRecordRepository.save(failedRecord);
                } catch (Exception saveEx) {
                    log.error("Failed to save FAILED price record for product id={}", product.getId(), saveEx);
                }
            }
        }

        log.info("Completed price check run for {} active products", activeProducts.size());
    }
}
