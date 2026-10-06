-- Registration storage contract (PostgreSQL 16, UTF-8). Copied verbatim to
-- backend/src/main/resources/db/migration/V1__create_registration.sql (AR-06, ES-08).
-- Timestamps are timestamptz written in UTC (AR-05); amounts are numeric(10,2) (D-09).

CREATE SEQUENCE registration_number_seq START WITH 1 INCREMENT BY 1 NO CYCLE;

CREATE TABLE registration (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    registration_number    VARCHAR(9)    NOT NULL,
    first_name             VARCHAR(100)  NOT NULL,
    last_name              VARCHAR(100)  NOT NULL,
    email                  VARCHAR(254)  NOT NULL,
    email_normalized       VARCHAR(254)  NOT NULL,
    payer_type             VARCHAR(10)   NOT NULL,
    company_name           VARCHAR(200),
    company_address        VARCHAR(500),
    company_vat_id         VARCHAR(30),
    workshop               VARCHAR(50),
    student                BOOLEAN       NOT NULL DEFAULT FALSE,
    net_fee                NUMERIC(10, 2) NOT NULL,
    vat                    NUMERIC(10, 2) NOT NULL,
    gross_fee              NUMERIC(10, 2) NOT NULL,
    registered_at          TIMESTAMPTZ   NOT NULL,
    confirmation_sent_at   TIMESTAMPTZ,
    confirmation_attempts  INTEGER       NOT NULL DEFAULT 0,
    CONSTRAINT uk_registration_number UNIQUE (registration_number),
    CONSTRAINT uk_registration_email UNIQUE (email_normalized),
    CONSTRAINT ck_registration_payer_type CHECK (payer_type IN ('private', 'company')),
    CONSTRAINT ck_registration_private_no_company CHECK (
        payer_type = 'company'
        OR (company_name IS NULL AND company_address IS NULL AND company_vat_id IS NULL)),
    CONSTRAINT ck_registration_company_fields CHECK (
        payer_type = 'private'
        OR (company_name IS NOT NULL AND company_address IS NOT NULL)),
    CONSTRAINT ck_registration_amounts CHECK (
        net_fee >= 0 AND vat >= 0 AND gross_fee = net_fee + vat)
);

CREATE INDEX ix_registration_unconfirmed
    ON registration (registered_at)
    WHERE confirmation_sent_at IS NULL;
