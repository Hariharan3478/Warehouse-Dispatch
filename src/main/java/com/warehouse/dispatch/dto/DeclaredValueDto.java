package com.warehouse.dispatch.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record DeclaredValueDto(
        @NotNull(message = "declaredValue.amount is mandatory")
        @PositiveOrZero(message = "declaredValue.amount must be positive or zero")
        BigDecimal amount,

        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter code")
        String currency
) {}