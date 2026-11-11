package com.warehouse.dispatch.util;

import com.warehouse.dispatch.dto.DimensionsDto;
import com.warehouse.dispatch.dto.PackageDto;
import com.warehouse.dispatch.dto.WeightDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeightCalculatorUtilTest {

    @Test
    @DisplayName("T1: Spec Example - 20x15x10 IN -> 50.800x38.100x25.400 cm; 11 LB -> 4.990 kg")
    void testSpecExampleT1() {
        assertEquals(new BigDecimal("50.800"), WeightCalculatorUtil.toCentimeters(new BigDecimal("20"), "IN"));
        assertEquals(new BigDecimal("38.100"), WeightCalculatorUtil.toCentimeters(new BigDecimal("15"), "IN"));
        assertEquals(new BigDecimal("25.400"), WeightCalculatorUtil.toCentimeters(new BigDecimal("10"), "IN"));
        assertEquals(new BigDecimal("4.990"), WeightCalculatorUtil.toKilograms(new BigDecimal("11"), "LB"));
    }

    @Test
    @DisplayName("T2: Spec Rounding - 9.832 -> 10.0; 18.000 -> 18.0; 5.400 -> 5.5")
    void testSpecRoundingT2() {
        assertEquals(new BigDecimal("10.0"), WeightCalculatorUtil.roundUpToHalfKg(new BigDecimal("9.832")));
        assertEquals(new BigDecimal("18.0"), WeightCalculatorUtil.roundUpToHalfKg(new BigDecimal("18.000")));
        assertEquals(new BigDecimal("5.5"), WeightCalculatorUtil.roundUpToHalfKg(new BigDecimal("5.400")));
    }

    @Test
    @DisplayName("T2: Package chargeableKg selects max(actual, volumetric) and ceils to 0.5 kg")
    void testPackageChargeableKg() {
        // 50.800 x 38.100 x 25.400 / 5000 = 9.832 kg volumetric; actual 4.990 kg -> max is 9.832 -> rounds to 10.0
        DimensionsDto dims = new DimensionsDto(new BigDecimal("20"), new BigDecimal("15"), new BigDecimal("10"), "IN");
        WeightDto weight = new WeightDto(new BigDecimal("11"), "LB");
        PackageDto pkg = new PackageDto("PKG-1", dims, weight, false, false);

        assertEquals(new BigDecimal("10.0"), WeightCalculatorUtil.calculatePackageChargeableKg(pkg));
    }

    @Test
    @DisplayName("T2: Order chargeableKg sums all packages' chargeableKg")
    void testOrderChargeableSum() {
        DimensionsDto dims1 = new DimensionsDto(new BigDecimal("20"), new BigDecimal("15"), new BigDecimal("10"), "IN");
        WeightDto weight1 = new WeightDto(new BigDecimal("11"), "LB"); // 10.0 kg

        DimensionsDto dims2 = new DimensionsDto(new BigDecimal("50"), new BigDecimal("40"), new BigDecimal("30"), "CM");
        WeightDto weight2 = new WeightDto(new BigDecimal("18"), "KG"); // 18.0 kg

        PackageDto p1 = new PackageDto("P1", dims1, weight1, false, false);
        PackageDto p2 = new PackageDto("P2", dims2, weight2, false, false);

        BigDecimal total = WeightCalculatorUtil.calculateOrderChargeableKg(List.of(p1, p2));
        assertEquals(new BigDecimal("28.0"), total);
    }
}