package com.pricepulse.pricing.service;

import com.pricepulse.pricing.entity.CheckStatus;

import java.math.BigDecimal;

/**
 * Value object returned by {@link PriceCheckerService#check(String)}.
 *
 * <ul>
 *   <li>On SUCCESS: {@code price} and {@code currency} are non-null; {@code errorMessage} is null.</li>
 *   <li>On FAILED:  {@code price} and {@code currency} are null; {@code errorMessage} is non-null.</li>
 * </ul>
 *
 * Instances are created only via the static factory methods {@link #success} and {@link #failed}.
 */
public record PriceResult(
        BigDecimal price,
        String currency,
        CheckStatus status,
        String errorMessage
) {

    /**
     * Factory for a successful price check.
     *
     * @param price    the fetched price (must not be null)
     * @param currency the currency code, e.g. "USD" (must not be null)
     * @return a SUCCESS PriceResult
     */
    public static PriceResult success(BigDecimal price, String currency) {
        return new PriceResult(price, currency, CheckStatus.SUCCESS, null);
    }

    /**
     * Factory for a failed price check.
     *
     * @param errorMessage a human-readable description of the failure (must not be null)
     * @return a FAILED PriceResult
     */
    public static PriceResult failed(String errorMessage) {
        return new PriceResult(null, null, CheckStatus.FAILED, errorMessage);
    }
}
