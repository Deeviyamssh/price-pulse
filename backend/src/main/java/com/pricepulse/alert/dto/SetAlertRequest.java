package com.pricepulse.alert.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SetAlertRequest(
        @NotNull(message = "Target price must be provided")
        @DecimalMin(value = "0.01", message = "Target price must be greater than zero")
        @DecimalMax(value = "999999999.99", message = "Target price must not exceed 999999999.99")
        @Digits(integer = 9, fraction = 2, message = "Target price must have at most 2 decimal places")
        BigDecimal targetPrice
) {}
