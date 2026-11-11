package com.warehouse.dispatch.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OrderDetailResponse(
        String orderNo,
        String batchId,
        String loadId,
        String zoneCode,
        String serviceLevel,
        String carrierCode,
        BigDecimal chargeableKg,
        BigDecimal allocatedFreight,
        BigDecimal discount,
        BigDecimal insuranceCharge,
        BigDecimal orderCharge,
        LocalDate etaDate
) {}