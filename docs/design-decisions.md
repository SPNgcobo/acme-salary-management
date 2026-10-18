# ACME Salary Management System: Design Decisions

Record of the key technical decisions for this project, their reasoning, and
their current status. A decision may be *Accepted* (chosen) while its
*Implementation* is still *Pending*.

Legend for status:
- **Accepted** — decision is settled.
- **Implemented** — code exists and builds.
- **Verified** — covered by passing automated tests or observed behaviour.
- **Pending** — not yet started.

---

## Decision 1: Modular Monolith
**Status:** Accepted
**Decision:** One Spring Boot application divided into employee, salary,
reporting, security, and common modules.
**Reason:** The stated scope does not justify the operational complexity of
microservices. One deployable unit is simpler to test, run, and demonstrate.
**Trade-off:** Module boundaries must be maintained intentionally to avoid a
tangled monolith.
**Implementation:** Pending.

## Decision 2: Java 21 and Spring Boot 4.1.1
**Status:** Accepted
**Decision:** Java 21 LTS and Spring Boot 4.1.1 for the backend.
**Reason:** LTS toolchain, mature ecosystem for REST, validation, DI,
persistence, and testing. Spring Boot 4 gives current security defaults and
testing support for a modern codebase.
**Trade-off:** Spring Boot 4 modularised its test starters and moved some
annotations (`@DataJpaTest` is now in
`org.springframework.boot.data.jpa.test.autoconfigure`), so migration effort
is real but manageable. Accepted because we want current defaults.
**Implementation:** Implemented — application builds and runs on 4.1.1;
imports corrected for the new package layout.

## Decision 3: PostgreSQL
**Status:** Accepted
**Decision:** PostgreSQL 17 as the relational database.
**Reason:** Strong relational integrity, constraints, transactions, and
analytical SQL for reports. Also: free managed hosting exists (e.g. Neon),
so the same schema applies to local development and the deployed
environment.
**Trade-off:** Requires a running server (vs. an embedded file DB). Chosen
for realistic behaviour and free managed hosting at deploy time.
**Implementation:** Implemented — connection verified via Hikari; Flyway
migration applied against `acme_salary_management` (PostgreSQL 17.6).
**Least-privilege role:** the application connects as a dedicated `acme_app`
role (not the `postgres` superuser), with grants limited to the app's own
database and schema. Migration grants were scoped to the app's database.

## Decision 4: Flyway for Migrations
**Status:** Accepted
**Decision:** Versioned SQL migrations managed by Flyway.
**Reason:** Schema changes become repeatable, reviewable, and traceable.
**Rule:** Never modify an applied migration — create a new one.
**Trade-off:** Slightly more ceremony than `ddl-auto: update`, but far safer.
**Implementation:** Implemented — `V1__create_employees.sql` applies on
startup; Hibernate runs in `validate` mode and fails fast if entity and
schema disagree.

## Decision 5: Controller / Service / Repository Layers
**Status:** Accepted
**Decision:** Controllers handle HTTP, services hold business rules,
repositories handle persistence.
**Reason:** Clear separation makes the code easier to test and reason about.
**Trade-off:** More classes, more indirection; accepted because the domain
(salary history, currency rules) has real business logic worth isolating.
**Implementation:** Pending — only the repository layer exists so far
(`EmployeeRepository`).

## Decision 6: Validate at Multiple Levels
**Status:** Accepted
**Decision:** Jakarta Bean Validation on requests **and** database
constraints for integrity.
**Reason:** Request validation gives friendly errors; database constraints
guarantee correctness even under concurrency.
**Trade-off:** Duplicated rules must be kept in sync.
**Implementation:** Partially — database constraints are in place (`CHECK`
on country code and employment status, `UNIQUE` on employee number and
email). Bean Validation on request DTOs is pending.

## Decision 7: Consistent API Error Responses
**Status:** Accepted
**Decision:** A single error response shape via a global exception handler.
**Reason:** Predictable errors for clients (validation, not-found, conflict,
unexpected).
**Trade-off:** A shared shape can feel heavy for trivial cases; accepted for
consistency.
**Implementation:** Pending.

## Decision 8: Server-Side Pagination, Sorting, Search, Filtering
**Status:** Accepted
**Decision:** Paginate, sort, search, and filter at the database query level
using Spring Data JPA `Specification` + `Pageable`.
**Reason:** 10,000 employees must not be loaded into memory or shipped to
the browser wholesale.
**Trade-off:** More complex query code; worth it for scalability and UX.
**Implementation:** Pending.

## Decision 9: Separate Employee Profiles from Salary History
**Status:** Accepted
**Decision:** Salary records are append-only, with `effective_date` and
`currency`. Never `UPDATE` a prior salary.
**Reason:** Salary history must be auditable; overwriting loses information.
**Trade-off:** Table grows over time; pagination and indexing handle it.
**Implementation:** Pending — `salary_records` will land in V2.

## Decision 10: Handle Currencies Explicitly
**Status:** Accepted
**Decision:** Every salary stores a currency code (ISO 4217). Reports group
by currency. No cross-currency sums without a documented conversion method.
**Reason:** Adding unlike currencies produces misleading figures.
**Trade-off:** Cross-currency roll-ups are deferred; users see correct
per-currency figures instead of a wrong single number.
**Implementation:** Pending.

## Decision 11: Credentials Outside Source Control
**Status:** Accepted
**Decision:** Secrets supplied through environment variables loaded from a
local `.env` file. `.env` is git-ignored; `.env.example` is committed as a
template with placeholder values.
**Reason:** Standard practice; prevents accidental credential leakage.
**Trade-off:** Requires local setup for each developer.
**Implementation:** Implemented — `.env` confirmed ignored via
`git check-ignore`; `.env.example` committed.

## Decision 12: Automated Tests, Run Against Real PostgreSQL
**Status:** Accepted
**Decision:** JUnit 5 + Spring Boot Test, focused on business rules and API
behaviour. Repository tests run against **real PostgreSQL**, not an
embedded H2 database. `@DataJpaTest` is annotated with
`@AutoConfigureTestDatabase(replace = Replace.NONE)`.
**Reason:** Flyway migrations, `TIMESTAMPTZ`, `BIGSERIAL`, and `CHECK`
constraints are Postgres-specific. Testing on H2 would hide dialect issues
and would not verify that the migrations actually apply.
**Trade-off:** Tests require a running PostgreSQL locally (and a managed
one or Testcontainers in CI) and run in ~2–4s per class instead of
~200ms. Accepted because correctness is more important than test speed for
a system whose core risk is data correctness (money, employment history).
**Implementation:** Implemented — `EmployeeRepositoryTest` (5 tests) and
`SalaryManagementApplicationTests` (1 test) pass with this configuration.
**Follow-up:** add Testcontainers so CI does not require a manually
provisioned Postgres.

## Decision 13: Protect Sensitive Data
**Status:** Accepted
**Decision:** Spring Security with JWT authentication protecting salary and
employee endpoints. Synthetic data only in dev and demo.
**Reason:** Salary data is confidential; unauthenticated write access is
unacceptable.
**Trade-off:** Adds a login step and token handling complexity for the demo;
accepted because it reflects real production requirements.
**Implementation:** Pending — default Spring Security auto-config is
currently active (in-memory user, generated password). Real JWT replaces it
in the security milestone.

## Decision 14: Containerise with Docker
**Status:** Accepted
**Decision:** Provide Dockerfiles for backend and frontend, plus a
`docker-compose.yml` that runs the full stack (Postgres + backend +
frontend) with one command.
**Reason:** Reproducible local setup and a clean deployment story. Removes
"works on my machine" risk for reviewers.
**Trade-off:** Slightly more setup; two additional image builds to maintain.
**Implementation:** Pending — added post-feature build per your instruction.