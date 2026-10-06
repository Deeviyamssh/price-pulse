package com.pricepulse.product.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for a product — used by the list endpoint ({@code GET /api/products}),
 * the single-product endpoint ({@code GET /api/products/{id}}), and the pause/resume
 * endpoints.
 *
 * <p>Fields follow the design's {@code ProductSummaryResponse} shape (Req 5.2, 14.3):
 * <ul>
 *   <li>{@code currentPrice}        — price from the most recent SUCCESS record; null if none</li>
 *   <li>{@code previousPrice}       — price from the second-most-recent SUCCESS record; null if &lt; 2</li>
 *   <li>{@code lowestRecordedPrice} — minimum price across all SUCCESS records; null if none</li>
 *   <li>{@code targetPrice}         — null if no PriceAlert is set</li>
 *   <li>{@code lastCheckedAt}       — timestamp of the most recent PriceRecord; null if none</li>
 *   <li>{@code lastCheckStatus}     — "SUCCESS" or "FAILED"; null if no PriceRecord exists</li>
 *   <li>{@code errorMessage}        — failure reason from most recent FAILED record; null on SUCCESS
 *       or when no record exists (Req 14.3 — dashboard must show error detail)</li>
 *   <li>{@code alertActive}         — false if no PriceAlert is set</li>
 * </ul>
 */
public record ProductResponse(
        Long id,
        String displayName,
        String url,
        String status,
        Instant createdAt,
        BigDecimal currentPrice,
        BigDecimal previousPrice,
        BigDecimal lowestRecordedPrice,
        BigDecimal targetPrice,
        Instant lastCheckedAt,
        String lastCheckStatus,
        String errorMessage,
        boolean alertActive
) {}
