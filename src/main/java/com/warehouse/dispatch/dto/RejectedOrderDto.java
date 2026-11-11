package com.warehouse.dispatch.dto;

public record RejectedOrderDto(
        String orderNo,
        String errorCode,
        String message
) {}