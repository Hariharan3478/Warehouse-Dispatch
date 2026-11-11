package com.warehouse.dispatch.api;

import com.warehouse.dispatch.dto.BatchDispatchRequest;
import com.warehouse.dispatch.dto.ConsolidationResponse;
import com.warehouse.dispatch.dto.OrderDetailResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/api/v2/dispatch")
public class DispatchController {

    @PostMapping("/consolidations")
    public ResponseEntity<?> consolidateBatch(
            @RequestHeader(value = "X-Warehouse-Key", required = true) String warehouseKey,
            @Valid @RequestBody BatchDispatchRequest request) {

        // Rule: Header warehouse key must match request warehouse code
        if (!warehouseKey.equals(request.warehouse().code())) {
            throw new IllegalArgumentException("X-Warehouse-Key header does not match warehouse.code");
        }

        // Rule: Date segment inside batchId (DSP-YYYYMMDD-XXXX) must match dispatchDate
        String dateSegment = request.batchId().substring(4, 12);
        String expectedDate = request.dispatchDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        if (!dateSegment.equals(expectedDate)) {
            throw new IllegalArgumentException("batchId date segment does not match dispatchDate");
        }

        // Rule: packageId must be unique within an individual order
        for (var order : request.orders()) {
            Set<String> pkgIds = new HashSet<>();
            for (var pkg : order.packages()) {
                if (!pkgIds.add(pkg.packageId())) {
                    throw new IllegalArgumentException("Duplicate packageId '" + pkg.packageId() + "' in order " + order.orderNo());
                }
            }
        }

        // Scaffold placeholder: returns 201 Created until service integration in Phase 4
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping("/consolidations/{batchId}")
    public ResponseEntity<ConsolidationResponse> getConsolidation(@PathVariable String batchId) {
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/orders/{orderNo}")
    public ResponseEntity<OrderDetailResponse> getOrder(@PathVariable String orderNo) {
        return ResponseEntity.notFound().build();
    }
}