-- Registration storage contract (docs/02_specification.md, section 5).
-- Applied unchanged as Flyway migration V1__create_registration.sql (AR-06, ES-08).
-- Database encoding UTF8 (NFR-01); timestamps in UTC (AR-05).

CREATE SEQUENCE registration_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE registration (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    registration_number VARCHAR(20)   NOT NULL UNIQUE,
    first_name          VARCHAR(100)  NOT NULL,
    last_name           VARCHAR(100)  NOT NULL,
    email               VARCHAR(254)  NOT NULL,
    payer_type          VARCHAR(10)   NOT NULL,
    company_name        VARCHAR(200),
    company_address     VARCHAR(500),
    company_vat_id      VARCHAR(20),
    workshop            VARCHAR(20),
    net_fee             NUMERIC(10, 2) NOT NULL,
    vat                 NUMERIC(10, 2) NOT NULL,
    gross_fee           NUMERIC(10, 2) NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT registration_number_format CHECK (registration_number ~ '^REG-[0-9]{6,}$'),
    CONSTRAINT registration_payer_type CHECK (payer_type IN ('private', 'company')),
    CONSTRAINT registration_company_fields CHECK (
        (payer_type = 'company' AND company_name IS NOT NULL AND company_address IS NOT NULL
            AND company_vat_id IS NOT NULL)
        OR (payer_type = 'private' AND company_name IS NULL AND company_address IS NULL
            AND company_vat_id IS NULL)
    ),
    CONSTRAINT registration_amounts CHECK (
        net_fee >= 0 AND vat >= 0 AND gross_fee = net_fee + vat
    )
);
