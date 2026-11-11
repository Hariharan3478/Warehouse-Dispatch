package com.warehouse.dispatch.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record LoadResponseDto(
        String loadId,
        String zoneCode,
        String serviceLevel,
        boolean hazmat,
        String carrierCode,
        BigDecimal totalChargeableKg,
        BigDecimal loadCost,
        LocalDate etaDate,
        List<OrderAllocationDto> orders
) {}