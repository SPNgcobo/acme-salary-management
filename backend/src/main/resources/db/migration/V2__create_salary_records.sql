-- V2: salary_records table
-- Append-only salary history. Every salary change creates a new row.
-- Never UPDATE a prior row. Never DELETE.

CREATE TABLE salary_records (
    id              BIGSERIAL     PRIMARY KEY,
    employee_id     BIGINT        NOT NULL,
    amount          NUMERIC(15,2) NOT NULL,
    currency_code   CHAR(3)       NOT NULL,
    effective_date  DATE          NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT fk_salary_records_employee
        FOREIGN KEY (employee_id) REFERENCES employees (id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_salary_records_employee_effective_date
        UNIQUE (employee_id, effective_date),

    CONSTRAINT ck_salary_records_amount_positive
        CHECK (amount > 0),

    CONSTRAINT ck_salary_records_currency_uppercase
        CHECK (currency_code ~ '^[A-Z]{3}$')
);

-- Index for current-salary lookup and history listing:
-- both operate on (employee_id, effective_date DESC).
CREATE INDEX idx_salary_records_employee_effective_desc
    ON salary_records (employee_id, effective_date DESC);

COMMENT ON TABLE  salary_records                    IS 'Append-only salary history.';
COMMENT ON COLUMN salary_records.amount             IS 'Salary amount, positive, cent precision.';
COMMENT ON COLUMN salary_records.currency_code      IS 'ISO 4217 alpha-3, uppercase.';
COMMENT ON COLUMN salary_records.effective_date     IS 'Date the salary becomes effective.';