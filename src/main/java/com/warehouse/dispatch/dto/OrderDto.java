package com.warehouse.dispatch.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OrderDto(
        @NotBlank(message = "orderNo is mandatory")
        String orderNo,

        @NotNull(message = "customer is mandatory")
        @Valid
        CustomerDto customer,

        @NotNull(message = "deliveryAddress is mandatory")
        @Valid
        DeliveryAddressDto deliveryAddress,

        @Pattern(regexp = "^(STANDARD|EXPRESS)$", message = "serviceLevel must be STANDARD or EXPRESS")
        String serviceLevel,

        @NotNull(message = "declaredValue is mandatory")
        @Valid
        DeclaredValueDto declaredValue,

        @NotEmpty(message = "packages list cannot be empty")
        @Size(min = 1, max = 20, message = "Each order must contain between 1 and 20 packages")
        @Valid
        List<PackageDto> packages
) {}