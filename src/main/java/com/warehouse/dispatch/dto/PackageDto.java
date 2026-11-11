package com.warehouse.dispatch.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PackageDto(
        @NotBlank(message = "packageId is mandatory")
        String packageId,

        @NotNull(message = "dimensions is mandatory")
        @Valid
        DimensionsDto dimensions,

        @NotNull(message = "weight is mandatory")
        @Valid
        WeightDto weight,

        boolean fragile,
        boolean hazmat
) {}