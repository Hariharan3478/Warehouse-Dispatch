package com.warehouse.dispatch.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record LoadResponseDto(
        String loadId,
        String zone,
        String serviceLevel,
        boolean hazmat,
        String carrier,
        BigDecimal chargeableKg,
        BigDecimal loadCost,
        LocalDate etaDate,
        List<OrderAllocationDto> orders
) {}