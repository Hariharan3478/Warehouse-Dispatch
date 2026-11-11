package com.warehouse.dispatch.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public final class PricingCalculatorUtil {

    private static final BigDecimal INSURANCE_THRESHOLD = new BigDecimal("50000.00");
    private static final BigDecimal INSURANCE_RATE = new BigDecimal("0.002"); // 0.2%
    private static final BigDecimal GOLD_DISCOUNT_RATE = new BigDecimal("0.05"); // 5%

    private PricingCalculatorUtil() {}

    /**
     * T9: Convert declared value to INR using FX rates for the dispatch date.
     * INR rate is 1.0. Foreign currencies use FX_RATE.
     */
    public static BigDecimal convertToInr(BigDecimal amount, String currency, Map<String, BigDecimal> fxRates) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (currency == null || "INR".equalsIgnoreCase(currency)) {
            return amount.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal rate = fxRates != null ? fxRates.get(currency.toUpperCase()) : null;
        if (rate == null) {
            throw new IllegalArgumentException("FX_RATE_MISSING for currency: " + currency);
        }
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * T9: Discount on allocated freight: GOLD 5%, SILVER 0%, BRONZE 0%.
     */
    public static BigDecimal calculateDiscount(BigDecimal allocatedFreight, String tier) {
        if (allocatedFreight == null || allocatedFreight.compareTo(BigDecimal.ZERO) <= 0 || tier == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        if ("GOLD".equalsIgnoreCase(tier)) {
            return allocatedFreight.multiply(GOLD_DISCOUNT_RATE).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * T9: insuranceCharge = 0.2% of declaredValueInr when declaredValueInr > 50000.00, else 0.00.
     */
    public static BigDecimal calculateInsuranceCharge(BigDecimal declaredValueInr) {
        if (declaredValueInr == null || declaredValueInr.compareTo(INSURANCE_THRESHOLD) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return declaredValueInr.multiply(INSURANCE_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * T9: orderCharge = allocatedFreight - discount + insuranceCharge
     */
    public static BigDecimal calculateOrderCharge(BigDecimal allocatedFreight, BigDecimal discount, BigDecimal insuranceCharge) {
        BigDecimal freight = allocatedFreight != null ? allocatedFreight : BigDecimal.ZERO;
        BigDecimal disc = discount != null ? discount : BigDecimal.ZERO;
        BigDecimal ins = insuranceCharge != null ? insuranceCharge : BigDecimal.ZERO;

        return freight.subtract(disc).add(ins).setScale(2, RoundingMode.HALF_UP);
    }
}