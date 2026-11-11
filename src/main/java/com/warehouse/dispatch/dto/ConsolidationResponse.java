package com.warehouse.dispatch.dto;

import java.time.LocalDate;
import java.util.List;

public record ConsolidationResponse(
        String batchId,
        String status,
        LocalDate effectiveDispatchDate,
        SummaryDto summary,
        List<LoadResponseDto> loads,
        List<RejectedOrderDto> rejectedOrders
) {}
