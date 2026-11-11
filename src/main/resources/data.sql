INSERT INTO ZONE_MAP (pincode_prefix, zone_code, active) VALUES
                                                             ('560', 'SOUTH-KA', 'Y'),
                                                             ('600', 'SOUTH-TN', 'Y'),
                                                             ('400', 'WEST-MH', 'Y'),
                                                             ('110', 'NORTH-DL', 'N');

INSERT INTO CARRIER_RATE (rate_id, carrier_code, zone_code, service_level, base_charge, rate_per_kg, fuel_pct, transit_days, hazmat_ok, reliability_score, effective_from, effective_to) VALUES
                                                                                                                                                                                             (1, 'SWIFTLINE', 'SOUTH-KA', 'STANDARD', 1200.00, 18.00, 10.00, 2, 'N', 92, DATE '2026-01-01', NULL),
                                                                                                                                                                                             (2, 'ROADKING',  'SOUTH-KA', 'STANDARD',  800.00, 22.00, 12.00, 3, 'Y', 88, DATE '2026-01-01', NULL),
                                                                                                                                                                                             (3, 'SWIFTLINE', 'SOUTH-KA', 'EXPRESS',  1500.00, 30.00, 10.00, 1, 'N', 92, DATE '2026-01-01', NULL),
                                                                                                                                                                                             (4, 'AIRNOVA',   'SOUTH-KA', 'EXPRESS',  2000.00, 26.00, 15.00, 1, 'N', 95, DATE '2026-01-01', NULL),
                                                                                                                                                                                             (5, 'ROADKING',  'SOUTH-TN', 'STANDARD',  500.00, 12.00, 12.00, 1, 'Y', 88, DATE '2026-01-01', NULL),
                                                                                                                                                                                             (6, 'SWIFTLINE', 'WEST-MH',  'STANDARD', 1400.00, 20.00, 10.00, 3, 'N', 92, DATE '2026-01-01', NULL);

INSERT INTO FX_RATE (currency, rate_date, inr_rate) VALUES
                                                        ('USD', DATE '2026-11-06', 84.1000),
                                                        ('EUR', DATE '2026-11-06', 91.2500);

INSERT INTO HOLIDAY (holiday_date, description) VALUES
                                                    (DATE '2026-11-09', 'Company holiday'),
                                                    (DATE '2026-12-25', 'Christmas');