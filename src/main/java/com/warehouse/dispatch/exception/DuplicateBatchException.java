package com.warehouse.dispatch.exception;

public class DuplicateBatchException extends RuntimeException {
    public DuplicateBatchException(String message) {
        super(message);
    }
}