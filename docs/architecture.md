# ACME Salary Management System — Architecture

> **Status:** Design document. All components are described by intent; each
> is marked `Pending` / `In progress` / `Implemented` / `Verified`. A section
> is only marked implemented when the code exists and builds; only marked
> verified when a test has been run and passed.

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
| Backend language | Java 21 LTS | Scaffolded |
| Backend framework | Spring Boot (latest stable) | Scaffolded |
| Database | PostgreSQL | Pending |
| Migrations | Flyway | Pending |
| Data access | Spring Data JPA | Pending |
| Validation | Jakarta Bean Validation | Pending |
| Security | Spring Security (JWT) | Pending |
| Testing | JUnit 5, Spring Boot Test | Pending |
| Frontend | Angular | Pending |
| Containerisation | Docker, Docker Compose | Pending |
| VCS | Git / GitHub | Implemented |

## 4. Backend Modules
- **Employee** — profile CRUD, directory listing, search, filter,
  pagination, sorting. *Pending.*
- **Salary** — amount, currency, effective date; append-only history.
  *Pending.*
- **Reporting** — dashboard summaries; per-currency and per-department
  aggregates. *Pending.*
- **Security** — JWT authentication and endpoint authorisation. *Pending.*
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

## 7. Data Model (planned)
- `employees` — id, employee_number (unique), first_name, last_name, email
  (unique), country_code, department, employment_status, created_at,
  updated_at.
- `salary_records` — id, employee_id (FK), amount (numeric), currency_code
  (ISO 4217), effective_date, created_at. Append-only.
- Indexes: employee_number, email, country_code, department,
  (employee_id, effective_date desc).

Exact columns and constraints are defined in the Flyway migration files.
**Status:** Pending.

## 8. Schema Management
Flyway versioned migrations. Hibernate is set to `validate` against the
managed schema — it must not generate or alter tables. Applied migrations
are immutable; changes are added as new versions. **Status:** Pending.

## 9. Reporting and Currency
No cross-currency sums. Reports group by currency, or apply a documented FX
conversion (rate source, rate date, methodology) when combining. v1 uses
per-currency grouping only. **Status:** Pending.

## 10. Security and Privacy
JWT-based authentication protects all salary and employee endpoints.
Credentials come from environment variables (loaded via `.env` locally,
managed as secrets at deployment). All development and demo data is
synthetic. **Status:** Pending.

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
- **Integration** — repository + database (Testcontainers or a dedicated
  test schema) for search, filters, pagination.
- **Frontend** — component and service tests.
- **End-to-end** — critical HR workflows, where practical.

Tests use synthetic data and must be deterministic. **Status:** Pending.

## 14. Synthetic Data
`scripts/` holds a repeatable generator producing 10,000 fictional
employees and their salary records. Must be deterministic (seeded RNG) and
must never contain real information. **Status:** Pending.

## 15. Deployment
Backend + database hosted (e.g. Render/Neon), frontend hosted (e.g. Vercel
or Netlify or the same Docker Compose stack on a VPS). Documented in
`README.md`. **Status:** Pending.