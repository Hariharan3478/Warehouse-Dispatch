package com.warehouse.dispatch.service;

import com.warehouse.dispatch.dto.*;
import com.warehouse.dispatch.util.WeightCalculatorUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class DispatchPersistenceWriter {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public DispatchPersistenceWriter(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    public void persistPlan(BatchDispatchRequest request, PlanResult plan) {
        Timestamp requestedAtUtc = Timestamp.valueOf(
                request.requestedAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime()
        );
        Timestamp createdAtUtc = Timestamp.from(Instant.now(clock));

        jdbcTemplate.update(
                "INSERT INTO DISPATCH_BATCH (batch_id, warehouse_code, dispatch_date, effective_dispatch_date, " +
                        "requested_at_utc, total_orders, accepted_orders, rejected_orders, total_loads, total_freight, status, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                plan.batchId(),
                request.warehouse().code().toUpperCase(),
                Date.valueOf(request.dispatchDate()),
                Date.valueOf(plan.effectiveDispatchDate()),
                requestedAtUtc,
                plan.summary().totalOrders(),
                plan.summary().acceptedOrders(),
                plan.summary().rejectedOrders(),
                plan.summary().totalLoads(),
                plan.summary().totalFreight(),
                plan.status(),
                createdAtUtc
        );

        if (!plan.loads().isEmpty()) {
            List<Object[]> loadParams = new ArrayList<>();
            for (PlanResult.PlannedLoad load : plan.loads()) {
                loadParams.add(new Object[]{
                        load.loadId(),
                        plan.batchId(),
                        load.zone(),
                        load.serviceLevel(),
                        load.hazmat() ? "Y" : "N",
                        load.carrier(),
                        load.packageCount(),
                        load.chargeableKg(),
                        load.baseCharge(),
                        load.weightCharge(),
                        load.fuelSurcharge(),
                        load.loadCost(),
                        Date.valueOf(load.etaDate())
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO DISPATCH_LOAD (load_id, batch_id, zone_code, service_level, hazmat, carrier_code, " +
                            "package_count, total_chargeable_kg, base_charge, weight_charge, fuel_surcharge, load_cost, eta_date) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    loadParams
            );

            List<Object[]> orderParams = new ArrayList<>();
            List<Object[]> pkgParams = new ArrayList<>();

            for (PlanResult.PlannedLoad load : plan.loads()) {
                for (PlanResult.PlannedOrderAllocation alloc : load.orders()) {
                    OrderDto o = alloc.originalOrder();
                    orderParams.add(new Object[]{
                            o.orderNo(),
                            load.loadId(),
                            plan.batchId(),
                            o.customer().code(),
                            o.customer().tier(),
                            alloc.deliveryAddressFormatted(),
                            o.deliveryAddress().pincode(),
                            o.serviceLevel(),
                            o.declaredValue().currency().toUpperCase(),
                            alloc.declaredValueInr(),
                            alloc.chargeableKg(),
                            alloc.allocatedFreight(),
                            alloc.discount(),
                            alloc.insuranceCharge(),
                            alloc.orderCharge()
                    });

                    for (PackageDto pkg : o.packages()) {
                        pkgParams.add(new Object[]{
                                o.orderNo(),
                                pkg.packageId(),
                                WeightCalculatorUtil.toCentimeters(pkg.dimensions().length(), pkg.dimensions().unit()),
                                WeightCalculatorUtil.toCentimeters(pkg.dimensions().width(), pkg.dimensions().unit()),
                                WeightCalculatorUtil.toCentimeters(pkg.dimensions().height(), pkg.dimensions().unit()),
                                WeightCalculatorUtil.toKilograms(pkg.weight().value(), pkg.weight().unit()),
                                WeightCalculatorUtil.calculateVolumetricKg(pkg.dimensions()),
                                WeightCalculatorUtil.calculatePackageChargeableKg(pkg),
                                pkg.fragile() ? "Y" : "N",
                                pkg.hazmat() ? "Y" : "N"
                        });
                    }
                }
            }

            jdbcTemplate.batchUpdate(
                    "INSERT INTO LOAD_ORDER (order_no, load_id, batch_id, customer_code, customer_tier, delivery_address, " +
                            "pincode, service_level, original_currency, declared_value_inr, chargeable_kg, allocated_freight, " +
                            "discount, insurance_charge, order_charge) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    orderParams
            );

            jdbcTemplate.batchUpdate(
                    "INSERT INTO ORDER_PACKAGE (order_no, package_id, length_cm, width_cm, height_cm, actual_kg, " +
                            "volumetric_kg, chargeable_kg, fragile, hazmat) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    pkgParams
            );
        }

        if (!plan.rejectedOrders().isEmpty()) {
            List<Object[]> errorParams = new ArrayList<>();
            for (RejectedOrderDto err : plan.rejectedOrders()) {
                errorParams.add(new Object[]{
                        plan.batchId(),
                        err.orderNo(),
                        err.errorCode(),
                        err.message()
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO BATCH_ERROR (batch_id, order_no, error_code, error_message) VALUES (?, ?, ?, ?)",
                    errorParams
            );
        }
    }

    public Optional<ConsolidationResponse> findBatchById(String batchId) {
        List<ConsolidationResponse> batches = jdbcTemplate.query(
                "SELECT batch_id, status, effective_dispatch_date, total_orders, accepted_orders, " +
                        "rejected_orders, total_loads, total_freight FROM DISPATCH_BATCH WHERE batch_id = ?",
                (rs, rowNum) -> {
                    SummaryDto summary = new SummaryDto(
                            rs.getInt("total_orders"),
                            rs.getInt("accepted_orders"),
                            rs.getInt("rejected_orders"),
                            rs.getInt("total_loads"),
                            rs.getBigDecimal("total_freight")
                    );
                    return new ConsolidationResponse(
                            rs.getString("batch_id"),
                            rs.getString("status"),
                            rs.getDate("effective_dispatch_date").toLocalDate(),
                            summary,
                            new ArrayList<>(),
                            new ArrayList<>()
                    );
                },
                batchId
        );

        if (batches.isEmpty()) {
            return Optional.empty();
        }

        ConsolidationResponse base = batches.get(0);

        List<LoadResponseDto> loads = jdbcTemplate.query(
                "SELECT load_id, zone_code, service_level, hazmat, carrier_code, total_chargeable_kg, load_cost, eta_date " +
                        "FROM DISPATCH_LOAD WHERE batch_id = ? ORDER BY load_id ASC",
                (rs, rowNum) -> {
                    String loadId = rs.getString("load_id");
                    List<OrderAllocationDto> orders = jdbcTemplate.query(
                            "SELECT order_no, chargeable_kg, allocated_freight, discount, insurance_charge, order_charge " +
                                    "FROM LOAD_ORDER WHERE load_id = ? ORDER BY chargeable_kg DESC, order_no ASC",
                            (ors, oRow) -> new OrderAllocationDto(
                                    ors.getString("order_no"),
                                    ors.getBigDecimal("chargeable_kg"),
                                    ors.getBigDecimal("allocated_freight"),
                                    ors.getBigDecimal("discount"),
                                    ors.getBigDecimal("insurance_charge"),
                                    ors.getBigDecimal("order_charge")
                            ),
                            loadId
                    );
                    return new LoadResponseDto(
                            loadId,
                            rs.getString("zone_code"),
                            rs.getString("service_level"),
                            "Y".equalsIgnoreCase(rs.getString("hazmat").trim()),
                            rs.getString("carrier_code"),
                            rs.getBigDecimal("total_chargeable_kg"),
                            rs.getBigDecimal("load_cost"),
                            rs.getDate("eta_date").toLocalDate(),
                            orders
                    );
                },
                batchId
        );

        List<RejectedOrderDto> rejectedOrders = jdbcTemplate.query(
                "SELECT order_no, error_code, error_message FROM BATCH_ERROR WHERE batch_id = ? ORDER BY error_id ASC",
                (rs, rowNum) -> new RejectedOrderDto(
                        rs.getString("order_no"),
                        rs.getString("error_code"),
                        rs.getString("error_message")
                ),
                batchId
        );

        return Optional.of(new ConsolidationResponse(
                base.batchId(),
                base.status(),
                base.effectiveDispatchDate(),
                base.summary(),
                loads,
                rejectedOrders
        ));
    }

    public Optional<OrderDetailResponse> findOrderByNo(String orderNo) {
        List<OrderDetailResponse> list = jdbcTemplate.query(
                "SELECT o.order_no, o.batch_id, o.load_id, l.zone_code, o.service_level, l.carrier_code, " +
                        "o.chargeable_kg, o.allocated_freight, o.discount, o.insurance_charge, o.order_charge, l.eta_date " +
                        "FROM LOAD_ORDER o JOIN DISPATCH_LOAD l ON o.load_id = l.load_id WHERE o.order_no = ?",
                (rs, rowNum) -> new OrderDetailResponse(
                        rs.getString("order_no"),
                        rs.getString("batch_id"),
                        rs.getString("load_id"),
                        rs.getString("zone_code"),
                        rs.getString("service_level"),
                        rs.getString("carrier_code"),
                        rs.getBigDecimal("chargeable_kg"),
                        rs.getBigDecimal("allocated_freight"),
                        rs.getBigDecimal("discount"),
                        rs.getBigDecimal("insurance_charge"),
                        rs.getBigDecimal("order_charge"),
                        rs.getDate("eta_date").toLocalDate()
                ),
                orderNo
        );
        return list.stream().findFirst();
    }
}
