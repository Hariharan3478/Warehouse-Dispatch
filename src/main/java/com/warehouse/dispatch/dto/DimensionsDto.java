package com.warehouse.dispatch.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record DimensionsDto(
        @NotNull(message = "length is mandatory")
        @Positive(message = "length must be positive")
        BigDecimal length,

        @NotNull(message = "width is mandatory")
        @Positive(message = "width must be positive")
        BigDecimal width,

        @NotNull(message = "height is mandatory")
        @Positive(message = "height must be positive")
        BigDecimal height,

        @Pattern(regexp = "^(CM|IN)$", message = "unit must be CM or IN")
        String unit
) {}