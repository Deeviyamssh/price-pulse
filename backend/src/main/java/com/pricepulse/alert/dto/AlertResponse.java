package com.pricepulse.alert.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AlertResponse(
        Long id,
        Long productId,
        BigDecimal targetPrice,
        boolean isActive,
        Instant lastNotifiedAt
) {}
