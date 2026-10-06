-- Registration storage contract (docs/02_specification.md section 8).
-- The backend's Flyway migration V1__create_registration.sql is identical to this file (AR-06).
-- Timestamps are stored in UTC (AR-05). Amounts in EUR with two decimals.

CREATE TABLE registration (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    registration_number VARCHAR(14)   NOT NULL,
    status              VARCHAR(20)   NOT NULL DEFAULT 'registered',
    first_name          VARCHAR(100)  NOT NULL,
    last_name           VARCHAR(100)  NOT NULL,
    email               VARCHAR(254)  NOT NULL,
    payer_type          VARCHAR(10)   NOT NULL,
    company_name        VARCHAR(200),
    company_address     VARCHAR(300),
    company_vat_id      VARCHAR(32),
    workshop            VARCHAR(20),
    net_fee             NUMERIC(10, 2) NOT NULL,
    vat                 NUMERIC(10, 2) NOT NULL,
    gross_fee           NUMERIC(10, 2) NOT NULL,
    submitted_at        TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uq_registration_number UNIQUE (registration_number),
    CONSTRAINT ck_registration_status CHECK (status IN ('registered')),
    CONSTRAINT ck_registration_payer_type CHECK (payer_type IN ('private', 'company')),
    CONSTRAINT ck_registration_company_data CHECK (
        (payer_type = 'company'
            AND company_name IS NOT NULL
            AND company_address IS NOT NULL
            AND company_vat_id IS NOT NULL)
        OR (payer_type = 'private'
            AND company_name IS NULL
            AND company_address IS NULL
            AND company_vat_id IS NULL)
    ),
    CONSTRAINT ck_registration_amounts CHECK (
        net_fee > 0 AND vat >= 0 AND gross_fee = net_fee + vat
    )
);
