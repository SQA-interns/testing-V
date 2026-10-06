-- Contract: registration storage (backend <-> PostgreSQL 16), interface "Registration storage".
-- Source: docs/02_specification.md section 5. Implemented by Flyway migration V1 (AR-06, ES-08).
-- Only the fields AC-001-07 lists plus the workshop selection are stored (SB-12, REQ-REG-01 "Privacy").
-- All timestamps are UTC (timestamptz, AR-05). Database encoding UTF8 (NFR-01).

CREATE TABLE registration (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    registration_number    VARCHAR(32)   NOT NULL,
    status                 VARCHAR(16)   NOT NULL DEFAULT 'registered',
    first_name             VARCHAR(100)  NOT NULL,
    last_name              VARCHAR(100)  NOT NULL,
    email                  VARCHAR(254)  NOT NULL,
    payer_type             VARCHAR(16)   NOT NULL,
    company_name           VARCHAR(200),
    company_address        VARCHAR(500),
    company_vat_id         VARCHAR(30),
    workshop               VARCHAR(32),
    net_fee                NUMERIC(10, 2) NOT NULL,
    vat                    NUMERIC(10, 2) NOT NULL,
    gross_fee              NUMERIC(10, 2) NOT NULL,
    submitted_at           TIMESTAMPTZ   NOT NULL,
    confirmation_status    VARCHAR(16)   NOT NULL DEFAULT 'pending',
    confirmation_attempts  INTEGER       NOT NULL DEFAULT 0,
    confirmation_sent_at   TIMESTAMPTZ,
    CONSTRAINT uk_registration_number UNIQUE (registration_number),
    CONSTRAINT ck_status CHECK (status IN ('registered')),
    CONSTRAINT ck_payer_type CHECK (payer_type IN ('private', 'company')),
    CONSTRAINT ck_company_data CHECK (
        (payer_type = 'company' AND company_name IS NOT NULL AND company_address IS NOT NULL
            AND company_vat_id IS NOT NULL)
        OR (payer_type = 'private' AND company_name IS NULL AND company_address IS NULL
            AND company_vat_id IS NULL)),
    CONSTRAINT ck_fees CHECK (net_fee >= 0 AND vat >= 0 AND gross_fee = net_fee + vat),
    CONSTRAINT ck_confirmation_status CHECK (confirmation_status IN ('pending', 'sent', 'failed'))
);

-- No index on email: repeated registrations are allowed and not detected (AC-001-09, NG4).
CREATE INDEX ix_registration_confirmation_pending
    ON registration (submitted_at)
    WHERE confirmation_status = 'pending';
