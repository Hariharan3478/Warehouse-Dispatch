CREATE TABLE IF NOT EXISTS ZONE_MAP (
                                        pincode_prefix CHAR(3) PRIMARY KEY,
    zone_code VARCHAR(12) NOT NULL,
    active CHAR(1) NOT NULL
    );

CREATE TABLE IF NOT EXISTS CARRIER_RATE (
                                            rate_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                            carrier_code VARCHAR(32) NOT NULL,
    zone_code VARCHAR(12) NOT NULL,
    service_level VARCHAR(16) NOT NULL,
    base_charge DECIMAL(10,2) NOT NULL,
    rate_per_kg DECIMAL(10,2) NOT NULL,
    fuel_pct DECIMAL(5,2) NOT NULL,
    transit_days INT NOT NULL,
    hazmat_ok CHAR(1) NOT NULL,
    reliability_score INT NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE
    );

CREATE TABLE IF NOT EXISTS FX_RATE (
                                       currency_code CHAR(3) PRIMARY KEY,
    to_inr_rate DECIMAL(10,4) NOT NULL
    );

CREATE TABLE IF NOT EXISTS HOLIDAY (
                                       holiday_date DATE PRIMARY KEY,
                                       description VARCHAR(64)
    );

CREATE TABLE IF NOT EXISTS DISPATCH_BATCH (
                                              batch_id VARCHAR(32) PRIMARY KEY,
    warehouse_code VARCHAR(16) NOT NULL,
    dispatch_date DATE NOT NULL,
    cutoff_time VARCHAR(8) NOT NULL,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
                               effective_dispatch_date DATE NOT NULL,
                               total_orders INT NOT NULL,
                               accepted_orders INT NOT NULL,
                               rejected_orders INT NOT NULL,
                               total_loads INT NOT NULL,
                               total_freight DECIMAL(12,2) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
                               );

CREATE TABLE IF NOT EXISTS DISPATCH_LOAD (
                                             load_id VARCHAR(40) PRIMARY KEY,
    batch_id VARCHAR(32) NOT NULL,
    zone_code VARCHAR(12) NOT NULL,
    service_level VARCHAR(16) NOT NULL,
    hazmat CHAR(1) NOT NULL,
    carrier_code VARCHAR(32) NOT NULL,
    chargeable_kg DECIMAL(10,2) NOT NULL,
    base_charge DECIMAL(10,2) NOT NULL,
    weight_charge DECIMAL(10,2) NOT NULL,
    fuel_surcharge DECIMAL(10,2) NOT NULL,
    load_cost DECIMAL(10,2) NOT NULL,
    eta_date DATE NOT NULL,
    FOREIGN KEY (batch_id) REFERENCES DISPATCH_BATCH(batch_id)
    );

CREATE TABLE IF NOT EXISTS LOAD_ORDER (
                                          load_order_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          load_id VARCHAR(40) NOT NULL,
    order_no VARCHAR(32) NOT NULL,
    customer_code VARCHAR(32) NOT NULL,
    customer_tier VARCHAR(16) NOT NULL,
    chargeable_kg DECIMAL(10,2) NOT NULL,
    allocated_freight DECIMAL(10,2) NOT NULL,
    discount DECIMAL(10,2) NOT NULL,
    insurance_charge DECIMAL(10,2) NOT NULL,
    order_charge DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (load_id) REFERENCES DISPATCH_LOAD(load_id)
    );

CREATE TABLE IF NOT EXISTS ORDER_PACKAGE (
                                             package_id VARCHAR(32) NOT NULL,
    order_no VARCHAR(32) NOT NULL,
    length_cm DECIMAL(8,2) NOT NULL,
    width_cm DECIMAL(8,2) NOT NULL,
    height_cm DECIMAL(8,2) NOT NULL,
    actual_kg DECIMAL(8,2) NOT NULL,
    volumetric_kg DECIMAL(8,2) NOT NULL,
    chargeable_kg DECIMAL(8,2) NOT NULL,
    fragile CHAR(1) NOT NULL,
    hazmat CHAR(1) NOT NULL,
    PRIMARY KEY (package_id, order_no)
    );

CREATE TABLE IF NOT EXISTS BATCH_ERROR (
                                           error_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           batch_id VARCHAR(32) NOT NULL,
    order_no VARCHAR(32) NOT NULL,
    error_code VARCHAR(32) NOT NULL,
    error_message VARCHAR(255) NOT NULL
    );