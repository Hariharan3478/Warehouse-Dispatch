package com.warehouse.dispatch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CustomerDto(
        @NotBlank(message = "customer.code is mandatory")
        String code,

        @Pattern(regexp = "^(GOLD|SILVER|BRONZE)$", message = "tier must be GOLD, SILVER, or BRONZE")
        String tier
) {}
