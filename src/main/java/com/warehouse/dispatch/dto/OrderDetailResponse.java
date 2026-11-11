package com.warehouse.dispatch.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OrderDetailResponse(
        String orderNo,
        String loadId,
        String carrier,
        BigDecimal orderCharge,
        LocalDate etaDate
) {}
