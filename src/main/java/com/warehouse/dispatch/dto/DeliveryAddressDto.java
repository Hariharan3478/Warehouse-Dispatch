package com.warehouse.dispatch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record DeliveryAddressDto(
        @NotBlank(message = "deliveryAddress.line1 is mandatory")
        String line1,

        @NotBlank(message = "deliveryAddress.city is mandatory")
        String city,

        @NotBlank(message = "deliveryAddress.state is mandatory")
        String state,

        @Pattern(regexp = "^\\d{6}$", message = "pincode must be exactly 6 digits")
        String pincode
) {}