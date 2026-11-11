package com.warehouse.dispatch.dto;

import java.math.BigDecimal;

public record OrderAllocationDto(
        String orderNo,
        BigDecimal chargeableKg,
        BigDecimal allocatedFreight,
        BigDecimal discount,
        BigDecimal insuranceCharge,
        BigDecimal orderCharge
) {}