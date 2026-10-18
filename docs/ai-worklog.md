# AI Work Log

A running record of how I used AI tools while building this project, the
suggestions I kept or rejected, and how I verified the result. The goal is to
show *intentional* AI use, not just AI use.

## Format
Each entry: **Date — task** / Prompt summary / AI output summary / My
decision / Verification / Commit.

---

### 2025-10-16 — Requirements document drafting
- **Prompt:** "Draft a one-page requirements doc for an HR salary management
  web app for 10,000 employees across multiple countries. Emphasise scope
  and out-of-scope reasoning."
- **AI output:** Suggested structure and a first draft of the out-of-scope
  list.
- **Decision:** Kept the structure. Rewrote the out-of-scope reasons in my
  own words and added the currency-handling constraint, which the AI did not
  raise.
- **Verification:** Reviewed against the assessment brief; every stated
  requirement maps to an in-scope item.
- **Commit:** `chore: initialize repository with Spring Boot scaffold, docs, and env template`

---

### 2025-10-17 — Architecture and design decisions
- **Prompt:** "Review my architecture and design decisions drafts and flag
  claims that are not yet backed by code."
- **AI output:** Flagged several entries that described test runs and
  implementation status for features that did not exist. Recommended a
  status legend (`Accepted` / `Implemented` / `Verified` / `Pending`) and
  marking everything unbuilt as `Pending`.
- **Decision:** Accepted. Rewrote both docs so statuses reflect reality.
  Added a containerisation decision (Docker + Compose) since the assessment
  expects a deployable result.
- **Verification:** Walked every "Implemented" / "Verified" claim against
  the actual repo — nothing claims completion without a commit or test log
  to back it.
- **Commit:** included with the repository initialisation commit.

---

### 2025-10-17 — Secrets handling and env template
- **Prompt:** "Best way to keep local secrets out of Git while making the
  project easy for reviewers to run."
- **AI output:** `.env` (git-ignored, real values) + `.env.example`
  (committed, placeholders). Also a `.gitignore` covering Maven, Node, IDE,
  and secrets.
- **Decision:** Accepted. Verified with `git check-ignore` before the first
  push.
- **Verification:** `git check-ignore .env` prints `.env`;
  `git check-ignore .env.example` prints nothing.
- **Commit:** `chore: initialize repository with Spring Boot scaffold, docs, and env template`

---

### 2025-10-17 — Employee entity, repository, Flyway V1 migration
- **Prompt:** "Design the first vertical slice: employee table migration,
  JPA entity, repository, and tests. Use Flyway + `ddl-auto=validate`."
- **AI output:** Suggested the columns, constraints, indexes, entity, and
  repository. Initial `country_code` check was a length check.
- **Decision:** Kept most of the schema. Tightened `country_code` to
  `CHECK (country_code ~ '^[A-Z]{2}$')` so lowercase values are rejected at
  the database. Kept explicit getters/setters over Lombok even though
  Lombok is on the classpath — reviewer readability.
- **Verification:** `./mvnw.cmd clean test` → `Tests run: 6, Failures: 0,
  Errors: 0, Skipped: 0` — BUILD SUCCESS.
- **Commit:** `feat(backend): add employee entity, repository, Flyway V1 migration, and repository tests`

---

### 2025-10-18 — Fixing Spring Boot 4 test infrastructure
- **Prompt:** "EmployeeRepositoryTest fails with `Cannot resolve symbol
  DataJpaTest` and then `Failed to replace DataSource with an embedded
  database for tests`."
- **AI output:** Two causes and two fixes:
  1. Spring Boot 4 moved `@DataJpaTest` from
     `org.springframework.boot.test.autoconfigure.orm.jpa` to
     `org.springframework.boot.data.jpa.test.autoconfigure`.
  2. `@DataJpaTest` tries to swap the configured datasource for H2 by
     default. Because H2 is not on the classpath, the test fails before it
     starts. Fix: add
     `@AutoConfigureTestDatabase(replace = Replace.NONE)` — or add H2.
- **Decision:** Accepted the import change. For the datasource, **deliberately
  rejected** adding H2 and chose real PostgreSQL via
  `Replace.NONE`. Rationale: Flyway migrations, `TIMESTAMPTZ`, `BIGSERIAL`,
  and `CHECK` constraints are Postgres-specific — testing on H2 would hide
  real dialect issues. Trade-off: tests need a running Postgres and are
  slower (~2–4s per class). Documented in `design-decisions.md` Decision 12.
- **Verification:** `EmployeeRepositoryTest` → exit code 0, 5 tests passing.
  `SalaryManagementApplicationTests` → exit code 0, 1 test passing. Combined
  `./mvnw.cmd clean test` → `Tests run: 6, Failures: 0, Errors: 0`.
- **Commit:** `test(backend): run @DataJpaTest against real Postgres (Replace.NONE); fix Spring Boot 4 import`

---

## Remaining Verification Work (tracked, not yet done)

- Employee service, controller, DTOs, and web-layer tests.
- Employee search/filter specification + tests.
- Salary module (V2 migration, append-only history, currency handling).
- Reporting module (per-currency, per-department).
- JWT authentication and endpoint protection.
- Angular frontend.
- Seed script for 10,000 employees.
- Dockerfiles and `docker-compose.yml`.
- Deployment and demo video.

## Rule

Never record a test, feature, or security control as verified unless the
check has actually been run and the output exists.