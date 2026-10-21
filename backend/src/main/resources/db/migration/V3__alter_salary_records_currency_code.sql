-- V3: normalise salary_records.currency_code from CHAR(3) to VARCHAR(3)
--
-- V2 declared currency_code as CHAR(3). CHAR is fixed-length and, in
-- PostgreSQL, uses the internal type `bpchar`. Hibernate's `validate` mode
-- compares the declared Java field type (String with length=3, i.e.
-- VARCHAR(3)) against the actual column and rejects a CHAR column as a
-- type mismatch.
--
-- VARCHAR(3) is the conventional Postgres choice for variable-length
-- textual codes, and the existing regex CHECK constraint already enforces
-- the three-uppercase-letter shape. Changing the underlying type preserves
-- both the length constraint (max 3) and the pattern check.

ALTER TABLE salary_records
    ALTER COLUMN currency_code TYPE VARCHAR(3)
    USING currency_code::varchar(3);