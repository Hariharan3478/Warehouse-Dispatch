package com.warehouse.dispatch.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record WeightDto(
        @NotNull(message = "weight.value is mandatory")
        @Positive(message = "weight.value must be positive")
        BigDecimal value,

        @Pattern(regexp = "^(KG|LB)$", message = "unit must be KG or LB")
        String unit
) {}
