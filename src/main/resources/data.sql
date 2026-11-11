MERGE INTO ZONE_MAP KEY(pincode_prefix) VALUES ('560', 'SOUTH-KA', 'Y');
MERGE INTO ZONE_MAP KEY(pincode_prefix) VALUES ('600', 'SOUTH-TN', 'Y');
MERGE INTO ZONE_MAP KEY(pincode_prefix) VALUES ('400', 'WEST-MH', 'Y');
MERGE INTO ZONE_MAP KEY(pincode_prefix) VALUES ('110', 'NORTH-DL', 'N');

MERGE INTO CARRIER_RATE KEY(carrier_code, zone_code, service_level, effective_from)
    VALUES (1, 'SWIFTLINE', 'SOUTH-KA', 'STANDARD', 1200.00, 18.00, 10.00, 2, 'N', 92, DATE '2026-01-01', NULL);

MERGE INTO CARRIER_RATE KEY(carrier_code, zone_code, service_level, effective_from)
    VALUES (2, 'ROADKING', 'SOUTH-KA', 'STANDARD', 800.00, 22.00, 12.00, 3, 'Y', 88, DATE '2026-01-01', NULL);

MERGE INTO CARRIER_RATE KEY(carrier_code, zone_code, service_level, effective_from)
    VALUES (3, 'SWIFTLINE', 'SOUTH-KA', 'EXPRESS', 1500.00, 30.00, 10.00, 1, 'N', 92, DATE '2026-01-01', NULL);

MERGE INTO CARRIER_RATE KEY(carrier_code, zone_code, service_level, effective_from)
    VALUES (4, 'AIRNOVA', 'SOUTH-KA', 'EXPRESS', 2000.00, 26.00, 15.00, 1, 'N', 95, DATE '2026-01-01', NULL);

MERGE INTO CARRIER_RATE KEY(carrier_code, zone_code, service_level, effective_from)
    VALUES (5, 'ROADKING', 'SOUTH-TN', 'STANDARD', 500.00, 12.00, 12.00, 1, 'Y', 88, DATE '2026-01-01', NULL);

MERGE INTO CARRIER_RATE KEY(carrier_code, zone_code, service_level, effective_from)
    VALUES (6, 'SWIFTLINE', 'WEST-MH', 'STANDARD', 1400.00, 20.00, 10.00, 3, 'N', 92, DATE '2026-01-01', NULL);

MERGE INTO FX_RATE KEY(currency_code) VALUES ('USD', 84.1000);
MERGE INTO FX_RATE KEY(currency_code) VALUES ('EUR', 91.2500);

MERGE INTO HOLIDAY KEY(holiday_date) VALUES (DATE '2026-11-09', 'Warehouse Maintenance');
MERGE INTO HOLIDAY KEY(holiday_date) VALUES (DATE '2026-12-25', 'Christmas Day');