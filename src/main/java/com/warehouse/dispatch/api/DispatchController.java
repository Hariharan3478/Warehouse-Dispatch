package com.warehouse.dispatch.api;

import com.warehouse.dispatch.dto.BatchDispatchRequest;
import com.warehouse.dispatch.dto.ConsolidationResponse;
import com.warehouse.dispatch.dto.OrderDetailResponse;
import com.warehouse.dispatch.dto.OrderDto;
import com.warehouse.dispatch.dto.PackageDto;
import com.warehouse.dispatch.service.DispatchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/api/v2/dispatch")
public class DispatchController {

    private static final DateTimeFormatter BATCH_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final DispatchService dispatchService;

    public DispatchController(DispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @PostMapping("/consolidations")
    public ResponseEntity<ConsolidationResponse> consolidateBatch(
            @RequestHeader(value = "X-Warehouse-Key", required = false) String warehouseKey,
            @Valid @RequestBody BatchDispatchRequest request) {

        validateHeaderAndStructuralRules(warehouseKey, request);
        ConsolidationResponse response = dispatchService.processBatch(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/consolidations/{batchId}")
    public ResponseEntity<ConsolidationResponse> getConsolidation(@PathVariable String batchId) {
        return dispatchService.getBatch(batchId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/orders/{orderNo}")
    public ResponseEntity<OrderDetailResponse> getOrderDetails(@PathVariable String orderNo) {
        return dispatchService.getOrder(orderNo)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private void validateHeaderAndStructuralRules(String warehouseKey, BatchDispatchRequest request) {
        if (warehouseKey == null || warehouseKey.isBlank() || !warehouseKey.equals(request.warehouse().code())) {
            throw new IllegalArgumentException("X-Warehouse-Key header does not match warehouse.code");
        }

        String datePartFromId = request.batchId().substring(4, 12);
        String expectedDatePart = request.dispatchDate().format(BATCH_DATE_FMT);
        if (!datePartFromId.equals(expectedDatePart)) {
            throw new IllegalArgumentException("batchId date segment does not match dispatchDate");
        }

        for (OrderDto order : request.orders()) {
            Set<String> seenPkgIds = new HashSet<>();
            for (PackageDto pkg : order.packages()) {
                if (!seenPkgIds.add(pkg.packageId())) {
                    throw new IllegalArgumentException("Duplicate packageId '" + pkg.packageId() + "' within order " + order.orderNo());
                }
            }
        }
    }
}