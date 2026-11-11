package com.warehouse.dispatch.service;

import com.warehouse.dispatch.dto.*;
import com.warehouse.dispatch.exception.AllOrdersRejectedException;
import com.warehouse.dispatch.exception.DuplicateBatchException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DispatchService {

    private final ReferenceDataLoader referenceDataLoader;
    private final DispatchPlannerEngine plannerEngine;
    private final DispatchPersistenceWriter persistenceWriter;

    public DispatchService(
            ReferenceDataLoader referenceDataLoader,
            DispatchPlannerEngine plannerEngine,
            DispatchPersistenceWriter persistenceWriter) {
        this.referenceDataLoader = referenceDataLoader;
        this.plannerEngine = plannerEngine;
        this.persistenceWriter = persistenceWriter;
    }

    @Transactional(noRollbackFor = AllOrdersRejectedException.class)
    public ConsolidationResponse processBatch(BatchDispatchRequest request) {
        if (referenceDataLoader.batchExists(request.batchId())) {
            throw new DuplicateBatchException("Batch already exists: " + request.batchId());
        }

        ReferenceData refData = referenceDataLoader.loadForBatch(request.dispatchDate());
        PlanResult plan = plannerEngine.plan(request, refData);

        persistenceWriter.persistPlan(request, plan);

        ConsolidationResponse response = mapToResponse(plan);

        if ("FAILED".equals(plan.status())) {
            throw new AllOrdersRejectedException(response);
        }

        return response;
    }

    @Transactional(readOnly = true)
    public Optional<ConsolidationResponse> getBatch(String batchId) {
        return persistenceWriter.findBatchById(batchId);
    }

    @Transactional(readOnly = true)
    public Optional<OrderDetailResponse> getOrder(String orderNo) {
        return persistenceWriter.findOrderByNo(orderNo);
    }

    private ConsolidationResponse mapToResponse(PlanResult plan) {
        List<LoadResponseDto> loadDtos = plan.loads().stream()
                .map(l -> new LoadResponseDto(
                        l.loadId(),
                        l.zone(),
                        l.serviceLevel(),
                        l.hazmat(),
                        l.carrier(),
                        l.chargeableKg(),
                        l.loadCost(),
                        l.etaDate(),
                        l.orders().stream()
                                .map(o -> new OrderAllocationDto(
                                        o.originalOrder().orderNo(),
                                        o.chargeableKg(),
                                        o.allocatedFreight(),
                                        o.discount(),
                                        o.insuranceCharge(),
                                        o.orderCharge()
                                ))
                                .toList()
                ))
                .toList();

        return new ConsolidationResponse(
                plan.batchId(),
                plan.status(),
                plan.effectiveDispatchDate(),
                plan.summary(),
                loadDtos,
                plan.rejectedOrders()
        );
    }
}
