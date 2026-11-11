package com.warehouse.dispatch.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PlanResult(
        String batchId,
        String status,
        LocalDate effectiveDispatchDate,
        SummaryDto summary,
        List<PlannedLoad> loads,
        List<RejectedOrderDto> rejectedOrders
) {
    public record PlannedLoad(
            String loadId,
            String zone,
            String serviceLevel,
            boolean hazmat,
            String carrier,
            int packageCount,
            BigDecimal chargeableKg,
            BigDecimal baseCharge,
            BigDecimal weightCharge,
            BigDecimal fuelSurcharge,
            BigDecimal loadCost,
            LocalDate etaDate,
            List<PlannedOrderAllocation> orders
    ) {}

    public record PlannedOrderAllocation(
            OrderDto originalOrder,
            BigDecimal chargeableKg,
            BigDecimal allocatedFreight,
            BigDecimal discount,
            BigDecimal declaredValueInr,
            BigDecimal insuranceCharge,
            BigDecimal orderCharge,
            String deliveryAddressFormatted
    ) {}
}
