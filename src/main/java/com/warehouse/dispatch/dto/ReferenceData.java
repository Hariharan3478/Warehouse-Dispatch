package com.warehouse.dispatch.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record ReferenceData(
        Map<String, String> activePincodeToZone, // 3-digit pincode prefix -> zone_code where active='Y'
        List<CarrierRateRecord> carrierRates,
        Map<String, BigDecimal> fxRatesForDate,  // currency -> inr_rate for dispatchDate
        Set<LocalDate> holidays,
        Set<String> existingOrderNumbers         // orders already in LOAD_ORDER
) {
    public record CarrierRateRecord(
            int rateId,
            String carrierCode,
            String zoneCode,
            String serviceLevel,
            BigDecimal baseCharge,
            BigDecimal ratePerKg,
            BigDecimal fuelPct,
            int transitDays,
            boolean hazmatOk,
            int reliabilityScore,
            LocalDate effectiveFrom,
            LocalDate effectiveTo
    ) {
        public boolean isEffectiveOn(LocalDate date) {
            if (date.isBefore(effectiveFrom)) {
                return false;
            }
            return effectiveTo == null || !date.isAfter(effectiveTo);
        }
    }
}