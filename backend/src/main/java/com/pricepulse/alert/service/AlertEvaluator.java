package com.pricepulse.alert.service;

import com.pricepulse.product.entity.Product;

import java.math.BigDecimal;

/**
 * Evaluates a successful price observation against the product's active alert.
 */
public interface AlertEvaluator {

    void evaluate(Product product, BigDecimal price);
}
