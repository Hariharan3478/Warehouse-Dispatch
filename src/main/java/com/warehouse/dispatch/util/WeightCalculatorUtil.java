package com.warehouse.dispatch.util;

import com.warehouse.dispatch.dto.DimensionsDto;
import com.warehouse.dispatch.dto.PackageDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class WeightCalculatorUtil {

    private static final BigDecimal INCH_TO_CM = new BigDecimal("2.54");
    private static final BigDecimal LB_TO_KG = new BigDecimal("0.45359237");
    private static final BigDecimal VOLUMETRIC_DIVISOR = new BigDecimal("5000");
    private static final BigDecimal HALF_KG_STEP = new BigDecimal("0.5");

    public static final BigDecimal MAX_ORDER_CHARGEABLE_KG = new BigDecimal("500.0");

    private WeightCalculatorUtil() {}

    /**
     * T1: Dimensions to centimetres: IN * 2.54. Round result to 3 decimals HALF_UP.
     */
    public static BigDecimal toCentimeters(BigDecimal value, String unit) {
        if ("IN".equalsIgnoreCase(unit)) {
            return value.multiply(INCH_TO_CM).setScale(3, RoundingMode.HALF_UP);
        }
        return value.setScale(3, RoundingMode.HALF_UP);
    }

    /**
     * T1: Weight to kilograms: LB * 0.45359237. Round result to 3 decimals HALF_UP.
     */
    public static BigDecimal toKilograms(BigDecimal value, String unit) {
        if ("LB".equalsIgnoreCase(unit)) {
            return value.multiply(LB_TO_KG).setScale(3, RoundingMode.HALF_UP);
        }
        return value.setScale(3, RoundingMode.HALF_UP);
    }

    /**
     * T2: volumetricKg = (lengthCm * widthCm * heightCm) / 5000, rounded to 3 decimals HALF_UP.
     */
    public static BigDecimal calculateVolumetricKg(DimensionsDto dims) {
        BigDecimal lengthCm = toCentimeters(dims.length(), dims.unit());
        BigDecimal widthCm = toCentimeters(dims.width(), dims.unit());
        BigDecimal heightCm = toCentimeters(dims.height(), dims.unit());

        BigDecimal volume = lengthCm.multiply(widthCm).multiply(heightCm);
        return volume.divide(VOLUMETRIC_DIVISOR, 3, RoundingMode.HALF_UP);
    }

    /**
     * T2: chargeableKg = max(actualKg, volumetricKg), then rounded UP to next 0.5 kg.
     */
    public static BigDecimal calculatePackageChargeableKg(PackageDto pkg) {
        BigDecimal actualKg = toKilograms(pkg.weight().value(), pkg.weight().unit());
        BigDecimal volKg = calculateVolumetricKg(pkg.dimensions());

        BigDecimal maxWeight = actualKg.max(volKg);
        return roundUpToHalfKg(maxWeight);
    }

    /**
     * Rounds UP to the next 0.5 kg (e.g., 9.832 -> 10.0; 18.000 -> 18.0; 5.400 -> 5.5).
     */
    public static BigDecimal roundUpToHalfKg(BigDecimal weight) {
        BigDecimal steps = weight.divide(HALF_KG_STEP, 0, RoundingMode.CEILING);
        return steps.multiply(HALF_KG_STEP).setScale(1, RoundingMode.UNNECESSARY);
    }

    /**
     * T2: Order chargeableKg = sum of its packages' chargeableKg.
     */
    public static BigDecimal calculateOrderChargeableKg(List<PackageDto> packages) {
        BigDecimal total = BigDecimal.ZERO;
        for (PackageDto pkg : packages) {
            total = total.add(calculatePackageChargeableKg(pkg));
        }
        return total.setScale(1, RoundingMode.HALF_UP);
    }
}