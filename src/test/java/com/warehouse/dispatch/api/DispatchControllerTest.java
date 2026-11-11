package com.warehouse.dispatch.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DispatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should return 400 when X-Warehouse-Key header does not match warehouse.code")
    void shouldReturn400OnHeaderMismatch() throws Exception {
        String json = """
        {
          "batchId": "DSP-20261106-0001",
          "dispatchDate": "2026-11-06",
          "requestedAt": "2026-11-06T10:00:00+05:30",
          "warehouse": { "code": "WH-CHN-01", "cutoffTime": "16:00" },
          "orders": [
            {
              "orderNo": "SO-101",
              "customer": { "code": "C1", "tier": "GOLD" },
              "deliveryAddress": { "line1": "Street", "city": "Bengaluru", "state": "KA", "pincode": "560001" },
              "serviceLevel": "STANDARD",
              "declaredValue": { "amount": 1000, "currency": "INR" },
              "packages": [
                {
                  "packageId": "P1",
                  "dimensions": { "length": 10, "width": 10, "height": 10, "unit": "CM" },
                  "weight": { "value": 1, "unit": "KG" },
                  "fragile": false,
                  "hazmat": false
                }
              ]
            }
          ]
        }
        """;

        mockMvc.perform(post("/api/v2/dispatch/consolidations")
                        .header("X-Warehouse-Key", "WRONG-KEY")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("X-Warehouse-Key header does not match warehouse.code"));
    }

    @Test
    @DisplayName("Should return 400 when batchId date segment does not match dispatchDate")
    void shouldReturn400OnBatchDateMismatch() throws Exception {
        String json = """
        {
          "batchId": "DSP-20261101-0001",
          "dispatchDate": "2026-11-06",
          "requestedAt": "2026-11-06T10:00:00+05:30",
          "warehouse": { "code": "WH-CHN-01", "cutoffTime": "16:00" },
          "orders": [
            {
              "orderNo": "SO-101",
              "customer": { "code": "C1", "tier": "GOLD" },
              "deliveryAddress": { "line1": "Street", "city": "Bengaluru", "state": "KA", "pincode": "560001" },
              "serviceLevel": "STANDARD",
              "declaredValue": { "amount": 1000, "currency": "INR" },
              "packages": [
                {
                  "packageId": "P1",
                  "dimensions": { "length": 10, "width": 10, "height": 10, "unit": "CM" },
                  "weight": { "value": 1, "unit": "KG" },
                  "fragile": false,
                  "hazmat": false
                }
              ]
            }
          ]
        }
        """;

        mockMvc.perform(post("/api/v2/dispatch/consolidations")
                        .header("X-Warehouse-Key", "WH-CHN-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("batchId date segment does not match dispatchDate"));
    }
}