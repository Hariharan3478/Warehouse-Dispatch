package com.warehouse.dispatch.service;

import com.warehouse.dispatch.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DispatchPlannerEngineTest {

    @Test
    @DisplayName("T1-T10: Reproduce Section 8 Worked Example exactly")
    void testSection8WorkedExample() {
        ReferenceData refData = new ReferenceData(
                Map.of("560", "SOUTH-KA", "600", "SOUTH-TN", "400", "WEST-MH"),
                List.of(
                        new ReferenceData.CarrierRateRecord(1, "SWIFTLINE", "SOUTH-KA", "STANDARD", new BigDecimal("1200.00"), new BigDecimal("18.00"), new BigDecimal("10.00"), 2, false, 92, LocalDate.of(2026, 1, 1), null),
                        new ReferenceData.CarrierRateRecord(2, "ROADKING", "SOUTH-KA", "STANDARD", new BigDecimal("800.00"), new BigDecimal("22.00"), new BigDecimal("12.00"), 3, true, 88, LocalDate.of(2026, 1, 1), null),
                        new ReferenceData.CarrierRateRecord(3, "SWIFTLINE", "SOUTH-KA", "EXPRESS", new BigDecimal("1500.00"), new BigDecimal("30.00"), new BigDecimal("10.00"), 1, false, 92, LocalDate.of(2026, 1, 1), null),
                        new ReferenceData.CarrierRateRecord(4, "AIRNOVA", "SOUTH-KA", "EXPRESS", new BigDecimal("2000.00"), new BigDecimal("26.00"), new BigDecimal("15.00"), 1, false, 95, LocalDate.of(2026, 1, 1), null)
                ),
                Map.of("USD", new BigDecimal("84.1000")),
                Set.of(LocalDate.of(2026, 11, 9)),
                Set.of()
        );

        OrderDto so1 = new OrderDto("SO-55001", new CustomerDto("C-1001", "GOLD"),
                new DeliveryAddressDto("12 Koramangala 4th Block", "Bengaluru", "KA", "560034"), "STANDARD",
                new DeclaredValueDto(new BigDecimal("30000.00"), "INR"),
                List.of(
                        new PackageDto("P1", new DimensionsDto(new BigDecimal("50"), new BigDecimal("40"), new BigDecimal("30"), "CM"), new WeightDto(new BigDecimal("18"), "KG"), false, false),
                        new PackageDto("P2", new DimensionsDto(new BigDecimal("20"), new BigDecimal("15"), new BigDecimal("10"), "IN"), new WeightDto(new BigDecimal("11"), "LB"), true, false)
                ));

        OrderDto so2 = new OrderDto("SO-55002", new CustomerDto("C-2040", "SILVER"),
                new DeliveryAddressDto("Plot 7, Whitefield", "Bengaluru", "KA", "560066"), "STANDARD",
                new DeclaredValueDto(new BigDecimal("1200.00"), "USD"),
                List.of(
                        new PackageDto("P1", new DimensionsDto(new BigDecimal("60"), new BigDecimal("50"), new BigDecimal("40"), "CM"), new WeightDto(new BigDecimal("25"), "KG"), false, false)
                ));

        OrderDto so3 = new OrderDto("SO-55003", new CustomerDto("C-3301", "SILVER"),
                new DeliveryAddressDto("MG Road", "Bengaluru", "KA", "560001"), "EXPRESS",
                new DeclaredValueDto(new BigDecimal("80000.00"), "INR"),
                List.of(
                        new PackageDto("P1", new DimensionsDto(new BigDecimal("30"), new BigDecimal("30"), new BigDecimal("30"), "CM"), new WeightDto(new BigDecimal("4"), "KG"), true, false)
                ));

        OrderDto so4 = new OrderDto("SO-55004", new CustomerDto("C-1001", "GOLD"),
                new DeliveryAddressDto("Unknown", "Nowhere", "XX", "999001"), "STANDARD",
                new DeclaredValueDto(new BigDecimal("500.00"), "INR"),
                List.of(
                        new PackageDto("P1", new DimensionsDto(new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"), "CM"), new WeightDto(new BigDecimal("1"), "KG"), false, false)
                ));

        BatchDispatchRequest request = new BatchDispatchRequest(
                "DSP-20261106-0007",
                LocalDate.of(2026, 11, 6),
                OffsetDateTime.parse("2026-11-06T11:20:00+05:30"),
                new WarehouseDto("WH-CHN-01", "16:00"),
                List.of(so1, so2, so3, so4)
        );

        DispatchPlannerEngine engine = new DispatchPlannerEngine();
        PlanResult result = engine.plan(request, refData);

        // Assert Batch Level (Section 8)
        assertEquals("PARTIAL", result.status());
        assertEquals(LocalDate.of(2026, 11, 6), result.effectiveDispatchDate());
        assertEquals(4, result.summary().totalOrders());
        assertEquals(3, result.summary().acceptedOrders());
        assertEquals(1, result.summary().rejectedOrders());
        assertEquals(2, result.summary().totalLoads());
        assertEquals(new BigDecimal("4033.42"), result.summary().totalFreight());

        // Assert Rejection (Section 8)
        assertEquals(1, result.rejectedOrders().size());
        assertEquals("SO-55004", result.rejectedOrders().get(0).orderNo());
        assertEquals("UNSERVICEABLE", result.rejectedOrders().get(0).errorCode());

        // Assert Loads (Section 8)
        assertEquals(2, result.loads().size());

        // Load 1: EXPRESS
        PlanResult.PlannedLoad load1 = result.loads().get(0);
        assertEquals("LD-DSP-20261106-0007-01", load1.loadId());
        assertEquals("SWIFTLINE", load1.carrier());
        assertEquals(new BigDecimal("5.5"), load1.chargeableKg());
        assertEquals(new BigDecimal("1831.50"), load1.loadCost());
        assertEquals(LocalDate.of(2026, 11, 7), load1.etaDate());

        // Load 2: STANDARD
        PlanResult.PlannedLoad load2 = result.loads().get(1);
        assertEquals("LD-DSP-20261106-0007-02", load2.loadId());
        assertEquals("ROADKING", load2.carrier());
        assertEquals(new BigDecimal("53.0"), load2.chargeableKg());
        assertEquals(new BigDecimal("2201.92"), load2.loadCost());
        assertEquals(LocalDate.of(2026, 11, 11), load2.etaDate());
    }
}