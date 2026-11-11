-- Reference (seeded)
CREATE TABLE ZONE_MAP (
                          pincode_prefix CHAR(3)     PRIMARY KEY,
                          zone_code      VARCHAR(12) NOT NULL,
                          active         CHAR(1)     NOT NULL
);

CREATE TABLE CARRIER_RATE (
                              rate_id           INT           PRIMARY KEY,
                              carrier_code      VARCHAR(15)   NOT NULL,
                              zone_code         VARCHAR(12)   NOT NULL,
                              service_level     VARCHAR(10)   NOT NULL,
                              base_charge       DECIMAL(10,2) NOT NULL,
                              rate_per_kg       DECIMAL(8,2)  NOT NULL,
                              fuel_pct          DECIMAL(5,2)  NOT NULL,
                              transit_days      INT           NOT NULL,
                              hazmat_ok         CHAR(1)       NOT NULL,
                              reliability_score INT           NOT NULL,
                              effective_from    DATE          NOT NULL,
                              effective_to      DATE
);

CREATE TABLE FX_RATE (
                         currency  CHAR(3)       NOT NULL,
                         rate_date DATE          NOT NULL,
                         inr_rate  DECIMAL(10,4) NOT NULL,
                         PRIMARY KEY (currency, rate_date)
);

CREATE TABLE HOLIDAY (
                         holiday_date DATE        PRIMARY KEY,
                         description  VARCHAR(60) NOT NULL
);

-- Written by the service
CREATE TABLE DISPATCH_BATCH (
                                batch_id                VARCHAR(20)   PRIMARY KEY,
                                warehouse_code          VARCHAR(12)   NOT NULL,
                                dispatch_date           DATE          NOT NULL,
                                effective_dispatch_date DATE          NOT NULL,
                                requested_at_utc        TIMESTAMP     NOT NULL,
                                total_orders            INT           NOT NULL,
                                accepted_orders         INT           NOT NULL,
                                rejected_orders         INT           NOT NULL,
                                total_loads             INT           NOT NULL,
                                total_freight           DECIMAL(12,2) NOT NULL,
                                status                  VARCHAR(10)   NOT NULL,
                                created_at              TIMESTAMP     NOT NULL
);

CREATE TABLE DISPATCH_LOAD (
                               load_id             VARCHAR(30)   PRIMARY KEY,
                               batch_id            VARCHAR(20)   NOT NULL REFERENCES DISPATCH_BATCH(batch_id),
                               zone_code           VARCHAR(12)   NOT NULL,
                               service_level       VARCHAR(10)   NOT NULL,
                               hazmat              CHAR(1)       NOT NULL,
                               carrier_code        VARCHAR(15)   NOT NULL,
                               package_count       INT           NOT NULL,
                               total_chargeable_kg DECIMAL(8,1)  NOT NULL,
                               base_charge         DECIMAL(12,2) NOT NULL,
                               weight_charge       DECIMAL(12,2) NOT NULL,
                               fuel_surcharge      DECIMAL(12,2) NOT NULL,
                               load_cost           DECIMAL(12,2) NOT NULL,
                               eta_date            DATE          NOT NULL
);

CREATE TABLE LOAD_ORDER (
                            order_no           VARCHAR(15)   PRIMARY KEY,
                            load_id            VARCHAR(30)   NOT NULL REFERENCES DISPATCH_LOAD(load_id),
                            batch_id           VARCHAR(20)   NOT NULL REFERENCES DISPATCH_BATCH(batch_id),
                            customer_code      VARCHAR(10)   NOT NULL,
                            customer_tier      VARCHAR(6)    NOT NULL,
                            delivery_address   VARCHAR(200)  NOT NULL,
                            pincode            CHAR(6)       NOT NULL,
                            service_level      VARCHAR(10)   NOT NULL,
                            original_currency  CHAR(3)       NOT NULL,
                            declared_value_inr DECIMAL(14,2) NOT NULL,
                            chargeable_kg      DECIMAL(8,1)  NOT NULL,
                            allocated_freight  DECIMAL(12,2) NOT NULL,
                            discount           DECIMAL(12,2) NOT NULL,
                            insurance_charge   DECIMAL(12,2) NOT NULL,
                            order_charge       DECIMAL(12,2) NOT NULL
);

CREATE TABLE ORDER_PACKAGE (
                               id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                               order_no      VARCHAR(15)   NOT NULL REFERENCES LOAD_ORDER(order_no),
                               package_id    VARCHAR(10)   NOT NULL,
                               length_cm     DECIMAL(8,3)  NOT NULL,
                               width_cm      DECIMAL(8,3)  NOT NULL,
                               height_cm     DECIMAL(8,3)  NOT NULL,
                               actual_kg     DECIMAL(8,3)  NOT NULL,
                               volumetric_kg DECIMAL(8,3)  NOT NULL,
                               chargeable_kg DECIMAL(8,1)  NOT NULL,
                               fragile       CHAR(1)       NOT NULL,
                               hazmat        CHAR(1)       NOT NULL,
                               CONSTRAINT uq_pkg UNIQUE (order_no, package_id)
);

CREATE TABLE BATCH_ERROR (
                             error_id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                             batch_id      VARCHAR(20)   NOT NULL REFERENCES DISPATCH_BATCH(batch_id),
                             order_no      VARCHAR(15)   NOT NULL,
                             error_code    VARCHAR(30)   NOT NULL,
                             error_message VARCHAR(200)  NOT NULL
);