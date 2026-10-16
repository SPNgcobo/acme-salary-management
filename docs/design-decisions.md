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

## Decision 2: Java 21 and Spring Boot
**Status:** Accepted
**Decision:** Java 21 LTS and Spring Boot (latest stable) for the backend.
**Reason:** LTS toolchain, mature ecosystem for REST, validation, DI,
persistence, and testing.
**Trade-off:** Bleeding-edge Spring Boot versions can carry migration
friction; accepted because we want current defaults.
**Implementation:** Pending.

## Decision 3: PostgreSQL
**Status:** Accepted
**Decision:** PostgreSQL as the relational database.
**Reason:** Strong relational integrity, constraints, transactions, and
analytical SQL for reports.
**Trade-off:** Requires a running server (vs. an embedded file DB). Chosen
for realistic behaviour and free managed hosting options at deploy time.
**Implementation:** Pending.

## Decision 4: Flyway for Migrations
**Status:** Accepted
**Decision:** Versioned SQL migrations managed by Flyway.
**Reason:** Schema changes become repeatable, reviewable, and traceable.
**Rule:** Never modify an applied migration — create a new one.
**Trade-off:** Slightly more ceremony than `ddl-auto: update`, but far safer.
**Implementation:** Pending.

## Decision 5: Controller / Service / Repository Layers
**Status:** Accepted
**Decision:** Controllers handle HTTP, services hold business rules,
repositories handle persistence.
**Reason:** Clear separation makes the code easier to test and reason about.
**Trade-off:** More classes, more indirection; accepted because the domain
(salary history, currency rules) has real business logic worth isolating.
**Implementation:** Pending.

## Decision 6: Validate at Multiple Levels
**Status:** Accepted
**Decision:** Jakarta Bean Validation on requests **and** database
constraints for integrity.
**Reason:** Request validation gives friendly errors; database constraints
guarantee correctness even under concurrency.
**Trade-off:** Duplicated rules must be kept in sync.
**Implementation:** Pending.

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
**Implementation:** Pending.

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
template.
**Reason:** Standard practice; prevents accidental credential leakage.
**Trade-off:** Requires local setup for each developer.
**Implementation:** Pending.

## Decision 12: Automated Tests
**Status:** Accepted
**Decision:** JUnit 5 + Spring Boot Test, focused on business rules and API
behaviour.
**Reason:** Tests protect business rules (currency handling, salary history,
validation) from regressions.
**Trade-off:** Time investment upfront; pays back the moment the schema or
rules change.
**Implementation:** Pending.

## Decision 13: Protect Sensitive Data
**Status:** Accepted
**Decision:** Spring Security with JWT authentication protecting salary and
employee endpoints. Synthetic data only in dev and demo.
**Reason:** Salary data is confidential; unauthenticated write access is
unacceptable.
**Trade-off:** Adds a login step and token handling complexity for the demo;
accepted because it reflects real production requirements.
**Implementation:** Pending.

## Decision 14: Containerise with Docker
**Status:** Accepted
**Decision:** Provide Dockerfiles for backend and frontend, plus a
`docker-compose.yml` that runs the full stack (Postgres + backend +
frontend) with one command.
**Reason:** Reproducible local setup and a clean deployment story. Removes
"works on my machine" risk for reviewers.
**Trade-off:** Slightly more setup; two additional image builds to maintain.
**Implementation:** Pending — added post-feature build per your instruction.