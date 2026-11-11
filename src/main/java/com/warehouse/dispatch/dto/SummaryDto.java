package com.warehouse.dispatch.dto;

import java.math.BigDecimal;

public record SummaryDto(
        int totalOrders,
        int acceptedOrders,
        int rejectedOrders,
        int totalLoads,
        BigDecimal totalFreight
) {}