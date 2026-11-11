package com.warehouse.dispatch.service;

import com.warehouse.dispatch.dto.ReferenceData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

@Repository
public class ReferenceDataLoader {

    private final JdbcTemplate jdbcTemplate;

    public ReferenceDataLoader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean batchExists(String batchId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM DISPATCH_BATCH WHERE batch_id = ?",
                Integer.class,
                batchId
        );
        return count != null && count > 0;
    }

    public ReferenceData loadForBatch(LocalDate dispatchDate) {
        Map<String, String> activeZones = new HashMap<>();
        jdbcTemplate.query("SELECT pincode_prefix, zone_code, active FROM ZONE_MAP", rs -> {
            String prefix = rs.getString("pincode_prefix").trim();
            String zone = rs.getString("zone_code").trim();
            String active = rs.getString("active").trim();
            if ("Y".equalsIgnoreCase(active)) {
                activeZones.put(prefix, zone);
            }
        });

        List<ReferenceData.CarrierRateRecord> carrierRates = jdbcTemplate.query(
                "SELECT rate_id, carrier_code, zone_code, service_level, base_charge, rate_per_kg, " +
                        "fuel_pct, transit_days, hazmat_ok, reliability_score, effective_from, effective_to FROM CARRIER_RATE",
                (rs, rowNum) -> {
                    Date effTo = rs.getDate("effective_to");
                    return new ReferenceData.CarrierRateRecord(
                            rs.getInt("rate_id"),
                            rs.getString("carrier_code").trim(),
                            rs.getString("zone_code").trim(),
                            rs.getString("service_level").trim(),
                            rs.getBigDecimal("base_charge"),
                            rs.getBigDecimal("rate_per_kg"),
                            rs.getBigDecimal("fuel_pct"),
                            rs.getInt("transit_days"),
                            "Y".equalsIgnoreCase(rs.getString("hazmat_ok").trim()),
                            rs.getInt("reliability_score"),
                            rs.getDate("effective_from").toLocalDate(),
                            effTo != null ? effTo.toLocalDate() : null
                    );
                }
        );

        Map<String, BigDecimal> fxRates = new HashMap<>();
        jdbcTemplate.query(
                "SELECT currency, inr_rate FROM FX_RATE WHERE rate_date = ?",
                rs -> {
                    fxRates.put(rs.getString("currency").trim().toUpperCase(), rs.getBigDecimal("inr_rate"));
                },
                Date.valueOf(dispatchDate)
        );

        Set<LocalDate> holidays = new HashSet<>();
        jdbcTemplate.query("SELECT holiday_date FROM HOLIDAY", rs -> {
            holidays.add(rs.getDate("holiday_date").toLocalDate());
        });

        Set<String> existingOrders = new HashSet<>(
                jdbcTemplate.queryForList("SELECT order_no FROM LOAD_ORDER", String.class)
        );

        return new ReferenceData(activeZones, carrierRates, fxRates, holidays, existingOrders);
    }
}
