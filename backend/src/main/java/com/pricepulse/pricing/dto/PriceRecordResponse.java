package com.pricepulse.pricing.dto;

import com.pricepulse.pricing.entity.CheckStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceRecordResponse(
        Long id,
        BigDecimal price,
        String currency,
        Instant checkedAt,
        CheckStatus checkStatus,
        String errorMessage
) {}
