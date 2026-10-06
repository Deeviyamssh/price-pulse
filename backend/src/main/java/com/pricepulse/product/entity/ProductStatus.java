package com.pricepulse.product.entity;

/**
 * Monitoring state of a {@link Product}.
 * <p>
 * {@code ACTIVE} — the scheduler includes this product in every price-check run.
 * {@code PAUSED} — the scheduler skips this product until it is resumed.
 */
public enum ProductStatus {
    ACTIVE,
    PAUSED
}
