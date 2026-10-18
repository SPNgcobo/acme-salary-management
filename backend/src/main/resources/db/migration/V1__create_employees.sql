-- V1: employees table
-- Employees carry the profile data. Salary records are added in V2.

CREATE TABLE employees (
    id                  BIGSERIAL PRIMARY KEY,
    employee_number     VARCHAR(20)  NOT NULL,
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    email               VARCHAR(255) NOT NULL,
    country_code        VARCHAR(2)   NOT NULL,
    department          VARCHAR(100) NOT NULL,
    employment_status   VARCHAR(20)  NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_employees_number UNIQUE (employee_number),
    CONSTRAINT uq_employees_email  UNIQUE (email),

    CONSTRAINT ck_employees_country_code
        CHECK (country_code ~ '^[A-Z]{2}$'),

    CONSTRAINT ck_employees_status
        CHECK (employment_status IN ('ACTIVE', 'ON_LEAVE', 'TERMINATED'))
);

-- Indexes for common search / filter patterns (directory listing).
CREATE INDEX idx_employees_last_name       ON employees (last_name);
CREATE INDEX idx_employees_country         ON employees (country_code);
CREATE INDEX idx_employees_department      ON employees (department);
CREATE INDEX idx_employees_status          ON employees (employment_status);
CREATE INDEX idx_employees_name_search     ON employees (LOWER(first_name), LOWER(last_name));

COMMENT ON TABLE  employees                    IS 'Employee profile records.';
COMMENT ON COLUMN employees.country_code       IS 'ISO 3166-1 alpha-2 country code (uppercase).';
COMMENT ON COLUMN employees.employment_status  IS 'ACTIVE | ON_LEAVE | TERMINATED';