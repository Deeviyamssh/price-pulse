package com.pricepulse.pricing.entity;

/**
 * Represents the outcome of a single price-check attempt.
 * SUCCESS means the price was retrieved successfully.
 * FAILED means the check encountered an error (network, parsing, etc.).
 */
public enum CheckStatus {
    SUCCESS,
    FAILED
}
