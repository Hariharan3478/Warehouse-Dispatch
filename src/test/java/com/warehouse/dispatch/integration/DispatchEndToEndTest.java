package com.warehouse.dispatch.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.warehouse.dispatch.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DispatchEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("E2E: POST batch consolidations reproduces Section 8 results, persists to H2, and GET endpoints retrieve data")
    void testEndToEndDispatchPipeline() throws Exception {
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

        // 1. Post batch to consolidation endpoint with valid X-Warehouse-Key header
        mockMvc.perform(post("/api/v2/dispatch/consolidations")
                        .header("X-Warehouse-Key", "WH-CHN-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.batchId").value("DSP-20261106-0007"))
                .andExpect(jsonPath("$.status").value("PARTIAL"))
                .andExpect(jsonPath("$.summary.totalOrders").value(4))
                .andExpect(jsonPath("$.summary.acceptedOrders").value(3))
                .andExpect(jsonPath("$.summary.rejectedOrders").value(1))
                .andExpect(jsonPath("$.summary.totalLoads").value(2))
                .andExpect(jsonPath("$.summary.totalFreight").value(4033.42))
                .andExpect(jsonPath("$.rejectedOrders[0].orderNo").value("SO-55004"))
                .andExpect(jsonPath("$.rejectedOrders[0].errorCode").value("UNSERVICEABLE"));

        // 2. Test GET /api/v2/dispatch/consolidations/{batchId}
        mockMvc.perform(get("/api/v2/dispatch/consolidations/DSP-20261106-0007"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchId").value("DSP-20261106-0007"))
                .andExpect(jsonPath("$.status").value("PARTIAL"));

        // 3. Test GET /api/v2/dispatch/orders/{orderNo} for accepted order SO-55001
        mockMvc.perform(get("/api/v2/dispatch/orders/SO-55001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNo").value("SO-55001"))
                .andExpect(jsonPath("$.batchId").value("DSP-20261106-0007"));
    }
}