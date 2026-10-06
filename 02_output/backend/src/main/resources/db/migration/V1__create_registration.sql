-- Registration storage (docs/02_contracts/registration-storage.sql, AR-06, ES-08).

CREATE SEQUENCE registration_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE registration (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    registration_number VARCHAR(20)   NOT NULL,
    first_name          VARCHAR(100)  NOT NULL,
    last_name           VARCHAR(100)  NOT NULL,
    email               VARCHAR(254)  NOT NULL,
    payer_type          VARCHAR(10)   NOT NULL,
    company_name        VARCHAR(200),
    company_address     VARCHAR(500),
    company_vat_id      VARCHAR(30),
    workshop            VARCHAR(20),
    net_fee             NUMERIC(10, 2) NOT NULL,
    vat                 NUMERIC(10, 2) NOT NULL,
    gross_fee           NUMERIC(10, 2) NOT NULL,
    created_at          TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uq_registration_number UNIQUE (registration_number),
    CONSTRAINT ck_payer_type CHECK (payer_type IN ('private', 'company')),
    CONSTRAINT ck_company_fields CHECK (
        (payer_type = 'company'
            AND company_name IS NOT NULL
            AND company_address IS NOT NULL
            AND company_vat_id IS NOT NULL)
        OR (payer_type = 'private'
            AND company_name IS NULL
            AND company_address IS NULL
            AND company_vat_id IS NULL)),
    CONSTRAINT ck_amounts CHECK (net_fee >= 0 AND vat >= 0 AND gross_fee = net_fee + vat)
);
