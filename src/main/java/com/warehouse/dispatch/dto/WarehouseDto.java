package com.warehouse.dispatch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record WarehouseDto(
        @NotBlank(message = "warehouse.code is mandatory")
        String code,

        @Pattern(regexp = "^(0[0-9]|1[0-9]|2[0-3]):[0-5][0-9]$", message = "cutoffTime must be in HH:mm format")
        String cutoffTime
) {}