package com.warehouse.dispatch.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PricingCalculatorUtilTest {

    private final Map<String, BigDecimal> fxRates = Map.of(
            "USD", new BigDecimal("84.1000"),
            "EUR", new BigDecimal("91.2500")
    );

    @Test
    @DisplayName("T9: Currency conversion matches Section 8 example (USD 1200 * 84.10 = INR 100920.00)")
    void testCurrencyConversion() {
        BigDecimal usdVal = PricingCalculatorUtil.convertToInr(new BigDecimal("1200.00"), "USD", fxRates);
        assertEquals(new BigDecimal("100920.00"), usdVal);

        BigDecimal inrVal = PricingCalculatorUtil.convertToInr(new BigDecimal("30000.00"), "INR", fxRates);
        assertEquals(new BigDecimal("30000.00"), inrVal);

        assertThrows(IllegalArgumentException.class, () ->
                PricingCalculatorUtil.convertToInr(new BigDecimal("100.00"), "GBP", fxRates)
        );
    }

    @Test
    @DisplayName("T9: Discount rules - GOLD gets 5%, SILVER and BRONZE get 0%")
    void testDiscounts() {
        BigDecimal allocatedFreight = new BigDecimal("1163.28");
        BigDecimal goldDiscount = PricingCalculatorUtil.calculateDiscount(allocatedFreight, "GOLD");
        assertEquals(new BigDecimal("58.16"), goldDiscount);

        BigDecimal silverDiscount = PricingCalculatorUtil.calculateDiscount(allocatedFreight, "SILVER");
        assertEquals(new BigDecimal("0.00"), silverDiscount);

        BigDecimal bronzeDiscount = PricingCalculatorUtil.calculateDiscount(allocatedFreight, "BRONZE");
        assertEquals(new BigDecimal("0.00"), bronzeDiscount);
    }

    @Test
    @DisplayName("T9: Insurance surcharge - 0.2% on entire declaredValue when > 50000.00, else 0.00")
    void testInsurance() {
        assertEquals(new BigDecimal("0.00"), PricingCalculatorUtil.calculateInsuranceCharge(new BigDecimal("30000.00")));
        assertEquals(new BigDecimal("0.00"), PricingCalculatorUtil.calculateInsuranceCharge(new BigDecimal("50000.00")));

        assertEquals(new BigDecimal("201.84"), PricingCalculatorUtil.calculateInsuranceCharge(new BigDecimal("100920.00")));
        assertEquals(new BigDecimal("160.00"), PricingCalculatorUtil.calculateInsuranceCharge(new BigDecimal("80000.00")));
    }

    @Test
    @DisplayName("T9: Total order charge = allocatedFreight - discount + insuranceCharge")
    void testOrderCharge() {
        BigDecimal charge1 = PricingCalculatorUtil.calculateOrderCharge(
                new BigDecimal("1163.28"), new BigDecimal("58.16"), new BigDecimal("0.00")
        );
        assertEquals(new BigDecimal("1105.12"), charge1);

        BigDecimal charge2 = PricingCalculatorUtil.calculateOrderCharge(
                new BigDecimal("1038.64"), new BigDecimal("0.00"), new BigDecimal("201.84")
        );
        assertEquals(new BigDecimal("1240.48"), charge2);
    }
}