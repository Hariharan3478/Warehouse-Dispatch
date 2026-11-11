package com.warehouse.dispatch.exception;

import com.warehouse.dispatch.dto.ConsolidationResponse;

public class AllOrdersRejectedException extends RuntimeException {
    private final ConsolidationResponse response;

    public AllOrdersRejectedException(ConsolidationResponse response) {
        super("All orders in the batch were rejected");
        this.response = response;
    }

    public ConsolidationResponse getResponse() {
        return response;
    }
}