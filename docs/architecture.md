# ACME Salary Management System — Architecture

> **Status:** Design document. Each section is marked `Pending` /
> `In progress` / `Implemented` / `Verified`. Implemented means the code
> exists and builds; verified means a test has been run and passed.

## 1. Purpose
Enable ACME's HR Manager to manage employee records, maintain salary
history, and generate currency-aware reports across countries and
departments for 10,000 employees.

## 2. Architecture Style
**Modular monolith.** A single Spring Boot application divided into business
modules. Chosen over microservices to keep deployment, testing, and
demonstration simple while retaining clear module boundaries.

## 3. Technology Stack
| Layer | Technology | Status |
|---|---|---|
| Backend language | Java 21 LTS | Implemented |
| Backend framework | Spring Boot 4.1.1 | Implemented |
| Database | PostgreSQL 17 | Implemented |
| Migrations | Flyway | Implemented |
| Data access | Spring Data JPA + Hibernate | Implemented |
| Validation | Jakarta Bean Validation | Pending |
| Security | Spring Security (JWT) | Pending (auto-config active) |
| Testing | JUnit 5, Spring Boot Test | In progress |
| Frontend | Angular | Pending |
| Containerisation | Docker, Docker Compose | Pending |
| VCS | Git / GitHub | Implemented |

## 4. Backend Modules
- **Employee** — profile CRUD, directory listing, search, filter,
  pagination, sorting. *In progress:* entity, repository, V1 migration,
  and repository tests exist and pass; service, controller, DTOs, and search
  specification are pending.
- **Salary** — amount, currency, effective date; append-only history.
  *Pending.*
- **Reporting** — dashboard summaries; per-currency and per-department
  aggregates. *Pending.*
- **Security** — JWT authentication and endpoint authorisation. *Pending.*
  Current state: Spring Boot's default security auto-config is active (an
  in-memory user and generated password are logged at startup). Replaced in
  a later step with a real JWT filter.
- **Common** — shared error responses and exception handling. *Pending.*

## 5. Request Flow
HTTP request
→ Controller (parameter validation, HTTP concerns)
→ Service (business rules, currency rules, mapping to DTOs)
→ Repository (Spring Data JPA, Specification + Pageable)
→ PostgreSQL

## 6. Employee Search, Filtering, Pagination
Designed to support:
- Case-insensitive partial search on first name, last name, employee number,
  email.
- Filters on country code, department, employment status.
- Combination of search + filters in a single request.
- Server-side pagination and sorting with parameter validation.

Implementation approach: Spring Data JPA `Specification` predicates combined
with `Pageable`, executing at the database so only the requested page is
returned. **Status:** Pending.

## 7. Data Model
**Implemented in `V1__create_employees.sql`:**
- `employees` — id (BIGSERIAL PK), employee_number (unique), first_name,
  last_name, email (unique), country_code (ISO 3166-1 alpha-2 pattern
  check), department, employment_status (enum check), created_at,
  updated_at (both TIMESTAMPTZ).
- Indexes: `last_name`, `country_code`, `department`, `employment_status`,
  and `lower(first_name), lower(last_name)` for search.

**Planned for `V2__create_salary_records.sql`:**
- `salary_records` — id, employee_id (FK), amount (numeric), currency_code
  (ISO 4217), effective_date, created_at. Append-only.

## 8. Schema Management
Flyway versioned migrations. Hibernate is set to `validate` against the
managed schema — it does not create or alter tables. Applied migrations are
immutable; changes are added as new versions.

**Status:** Implemented. `V1__create_employees.sql` applies automatically
on startup and during test runs; Hibernate validates the `Employee` entity
against it. Verified by both test classes passing against real Postgres.

## 9. Reporting and Currency
No cross-currency sums. Reports group by currency, or apply a documented FX
conversion (rate source, rate date, methodology) when combining. v1 uses
per-currency grouping only. **Status:** Pending.

## 10. Security and Privacy
JWT-based authentication protects all salary and employee endpoints.
Credentials come from environment variables (loaded via `.env` locally,
managed as secrets at deployment). All development and demo data is
synthetic.

**Current status:** Spring Boot default security auto-config is active
(in-memory user, generated password logged at startup). JWT configuration
is pending — replaced in the security milestone.

## 11. Scalability and Performance
Server-side pagination and filtering against indexed columns keep list
endpoints responsive at 10,000 records. Performance claims require
measurement; none are asserted yet. **Status:** Pending.

## 12. Containerisation
- `backend/Dockerfile` — multi-stage build producing a slim JRE runtime
  image.
- `frontend/Dockerfile` — build stage (Angular) + nginx runtime.
- `docker-compose.yml` at repo root — Postgres + backend + frontend with a
  single `docker compose up`.
- `.env.docker` (committed example) supplies non-secret defaults; real
  secrets stay in `.env` (git-ignored).

**Status:** Planned; implemented after core features are working.

## 13. Testing Strategy
- **Unit** — service rules (currency, history, validation).
- **Web-layer** — controller behaviour via MockMvc.
- **Integration** — repository tests against real Postgres (see Decision 12
  in `design-decisions.md` — deliberate choice over H2).
- **Frontend** — component and service tests.
- **End-to-end** — critical HR workflows, where practical.

**Status:** In progress. Repository tests and the application context test
exist and pass against real Postgres. Service and controller tests pending.

## 14. Synthetic Data
`scripts/` holds a repeatable generator producing 10,000 fictional
employees and their salary records. Must be deterministic (seeded RNG) and
must never contain real information. **Status:** Pending.

## 15. Deployment
Backend + database hosted (e.g. Render + Neon), frontend hosted (e.g.
Vercel/Netlify or the same Docker Compose stack on a VPS). Documented in
`README.md`. **Status:** Pending.