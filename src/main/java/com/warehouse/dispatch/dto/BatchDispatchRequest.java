package com.warehouse.dispatch.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record BatchDispatchRequest(
        @Pattern(regexp = "^DSP-\\d{8}-\\d{4}$", message = "batchId must match format DSP-YYYYMMDD-XXXX")
        String batchId,

        @NotNull(message = "dispatchDate is mandatory")
        LocalDate dispatchDate,

        @NotNull(message = "requestedAt is mandatory")
        OffsetDateTime requestedAt,

        @NotNull(message = "warehouse is mandatory")
        @Valid
        WarehouseDto warehouse,

        @NotEmpty(message = "orders list cannot be empty")
        @Size(min = 1, max = 200, message = "A batch must contain between 1 and 200 orders")
        @Valid
        List<OrderDto> orders
) {}