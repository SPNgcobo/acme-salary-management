# ACME Salary Management System: AI Work Log

This document records how AI tools were used while building the ACME Salary Management System, which suggestions were accepted or rejected, and how the resulting work was verified.

The goal is to demonstrate **intentional, critical, and transparent AI use**, rather than AI use alone.

## Entry Format

Each entry records:

- **Date and task:** When the work took place and what was addressed.
- **Prompt summary:** The request given to the AI tool.
- **AI output:** The main suggestions or proposed solution.
- **Decision:** What was accepted, modified, or rejected, and why.
- **Verification:** Tests, checks, or manual validation performed.
- **Commit:** The related Git commit, where applicable.

---

## 2025-10-16 — Requirements Document Drafting

### Prompt

> Draft a one-page requirements document for an HR salary management web application for 10,000 employees across multiple countries. Emphasise scope and explain why certain features are out of scope.

### AI Output

Suggested a requirements document structure and an initial list of out-of-scope features.

### Decision

Kept the proposed structure but rewrote the out-of-scope explanations in my own words. Added a currency-handling constraint that was not adequately addressed in the initial suggestion.

### Verification

Reviewed the document against the assessment brief and checked that each stated requirement mapped to an in-scope item.

### Commit

`chore: initialize repository with Spring Boot scaffold, docs, and env template`

---

## 2025-10-17 — Architecture and Design Decisions

### Prompt

> Review my architecture and design decision drafts and flag claims that are not yet backed by code.

### AI Output

Flagged statements describing tests and implementation status for features that had not yet been implemented. Recommended a status vocabulary to distinguish accepted decisions, implemented work, verified behaviour, and pending work.

### Decision

Accepted the recommendation and revised the documents so that their status claims better reflected the repository. Added a containerisation decision covering Docker and Docker Compose.

### Verification

Reviewed implementation and verification claims against the actual repository and available test evidence.

### Commit

Included in the repository initialisation commit.

---

## 2025-10-17 — Secrets Handling and Environment Template

### Prompt

> What is the best way to keep local secrets out of Git while making the project easy for reviewers to run?

### AI Output

Recommended:

- A Git-ignored `.env` file for local credentials.
- A committed `.env.example` file containing placeholders.
- A `.gitignore` covering Maven, Node.js, IDE files, and secrets.

### Decision

Accepted the approach.

### Verification

Checked Git's ignore rules:

```powershell
git check-ignore .env
git check-ignore .env.example
```

Expected behaviour:

- `.env` is ignored.
- `.env.example` is not ignored.

### Commit

`chore: initialize repository with Spring Boot scaffold, docs, and env template`

---

## 2025-10-17 — Employee Entity, Repository, and Flyway V1 Migration

### Prompt

> Design the first vertical slice: employee table migration, JPA entity, repository, and tests. Use Flyway and ddl-auto=validate.

### AI Output

Suggested database columns, constraints, indexes, a JPA entity, and a repository. The initial country-code check validated only the string length.

### Decision

Kept most of the proposed schema but tightened the country-code constraint to validate the format:

```sql
CHECK (country_code ~ '^[A-Z]{2}$')
```

Kept explicit Java getters and setters rather than introducing Lombok.

### Verification

Ran the Maven test suite:

```powershell
.\mvnw.cmd clean test
```

**Recorded result at the time:**

```text
Tests run: 6, Failures: 0, Errors: 0
BUILD SUCCESS
```

### Commit

`feat(backend): add employee entity, repository, Flyway V1 migration, and repository tests`

---

## 2025-10-18 — Fixing Spring Boot 4 Test Infrastructure

### Prompt

> EmployeeRepositoryTest fails with Cannot resolve symbol DataJpaTest and then Failed to replace DataSource with an embedded database for tests.

### AI Output

Identified two issues and proposed corresponding fixes:

1. Spring Boot 4 changed the package used for `@DataJpaTest` to `org.springframework.boot.data.jpa.test.autoconfigure`.
2. `@DataJpaTest` attempts to replace the configured data source with an embedded database by default. Because H2 was not on the classpath, the test configuration needed adjustment.

The proposed options included using `@AutoConfigureTestDatabase(replace = Replace.NONE)` or adding H2.

### Decision

Accepted the import correction and deliberately rejected adding H2.

The project relies on PostgreSQL-specific behaviour, including Flyway migrations, `TIMESTAMPTZ`, `BIGSERIAL`, and check constraints. Testing against H2 could conceal database compatibility issues.

The trade-off is that repository tests require a running PostgreSQL instance. The decision was documented as Decision 12 in `docs/design-decisions.md`.

### Verification

Ran the combined test suite:

```powershell
.\mvnw.cmd clean test
```

**Recorded result at the time:**

```text
Tests run: 6, Failures: 0, Errors: 0
BUILD SUCCESS
```

### Commit

`test(backend): run @DataJpaTest against real Postgres (Replace.NONE); fix Spring Boot 4 import`

---

## 2025-10-19 — Employee API, DTOs, Error Handling, and Web Tests

### Prompt

> Design the employee API layer: request and response DTOs with validation, a service that handles business rules such as duplicate detection and not-found scenarios, a controller exposing POST and GET endpoints, and a consistent error response through a global exception handler. Add unit tests for the service and web-layer tests with MockMvc.

### AI Output

Suggested the following components and implementation approach.

**API components**

- `EmployeeRequest` record with `@NotBlank`, `@Email`, `@Pattern` for country code, and `@NotNull` for employment status.
- `EmployeeResponse` record with a static `from(Employee)` factory method.
- `EmployeeService` with transactional operations, duplicate checks, and input normalisation.
- `EmployeeController` exposing:
  - `POST /api/employees` — create an employee, returning HTTP 201.
  - `GET /api/employees/{id}` — retrieve an employee, returning HTTP 200 or 404.
  - `GET /api/employees` — list employees with pagination.
- `ApiErrorResponse` record and `GlobalExceptionHandler`.
- `EmployeeServiceTest` with five tests using a mocked repository.
- `EmployeeControllerTest` with five tests using `@WebMvcTest`, `@MockitoBean`, and MockMvc.

### Decisions and Modifications

Accepted the overall design, with the following implementation changes.

**Removed an unused import.**

Removed `import static org.mockito.ArgumentMatchers.eq;` from `EmployeeControllerTest` after the IDE flagged it as unused.

**Handled Spring Security in web tests.**

Added `@WithMockUser` and CSRF tokens using `.with(csrf())` to POST tests after observing HTTP 403 responses. This allowed the tests to exercise the controller under the configured security filters rather than bypassing those filters.

**Updated the Jackson import.**

Changed the test import from `com.fasterxml.jackson.databind.ObjectMapper` to `tools.jackson.databind.ObjectMapper` to match the Jackson 3 package used by the project's Spring Boot 4 configuration.

**Improved pagination serialisation.**

Added `WebConfig` with `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)` to address the `PageImpl` serialisation warning and provide a more stable paginated response structure.

**Added development-only security configuration.**

Added `DevSecurityConfig` under the development profile to permit requests and disable CSRF for local API testing through Postman or a browser. This configuration is intended for development only and must not be used as production security.

**Improved repository test isolation.**

Made `EmployeeRepositoryTest` use `@Transactional` for rollback after each test and UUID-based employee numbers after discovering that manually created Postman data could collide with test data in the shared PostgreSQL database.

### Verification

Ran the Maven test suite:

```powershell
.\mvnw.cmd clean test
```

**Recorded result at the time:**

```text
Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The reported test breakdown was:

| Test class | Tests |
|---|---:|
| `EmployeeControllerTest` | 5 |
| `EmployeeRepositoryTest` | 5 |
| `EmployeeServiceTest` | 5 |
| `SalaryManagementApplicationTests` | 1 |
| **Total** | **16** |

### Manual API Verification

The API was tested through Postman against a running application with the development profile enabled.

| # | Scenario | Expected result | Recorded result |
|---:|---|---|---|
| 1 | `GET` an empty employee list | HTTP 200 with an empty `content` array and pagination metadata | Matched |
| 2 | `POST` a valid employee (`EMP-0001`) | HTTP 201 with an employee representation and generated fields | Matched |
| 3 | `GET` the created employee by ID | HTTP 200 with the same employee | Matched |
| 4 | `POST` invalid data, including a blank employee number, invalid email, and lowercase country code | HTTP 400 with field-level validation details | Matched |
| 5 | `POST` a duplicate employee number | HTTP 409 with a duplicate-number error | Matched |
| 6 | `GET` a non-existent employee ID | HTTP 404 with an employee-not-found error | Matched |

Also checked in pgAdmin that the employee created through `POST` existed in the PostgreSQL `employees` table.

The Postman collection was committed at `docs/postman/acme-salary-management.postman_collection.json`, so reviewers can inspect and rerun the documented API scenarios.

### Commit

`feat(backend): add employee API layer with DTOs, service, controller, error handler, and web tests`

---

## 2025-10-20 — Employee Search, Filters, Pagination Validation, and Reproducible Configuration

This milestone extended the employee module with search, filtering, sort whitelisting, and paginated parameter validation. It also fixed the local configuration workflow so future milestones could start from a stable base.

### Part 1 — Search and Filters

#### Prompt

> Design the search and filter layer for the employee list endpoint. A single free-text search box that matches across names, employee number, and email is the HR Manager pattern; filters should be exact-match on country, department, and employment status, combinable with search. Enforce a sort-field whitelist and cap the page size.

#### AI Output

Suggested:

- `EmployeeSearchCriteria` as a small value object carrying validated search and filter values.
- `EmployeeSpecifications` as a composable specification factory, with one method per filter and a `fromCriteria(...)` combinator.
- `EmployeeService.list(EmployeeSearchCriteria, Pageable)` that falls back to `findAll(pageable)` when the criteria are empty.
- `EmployeeController.list(...)` with `@Validated` at the class level, `@Min`, `@Max`, `@Size`, and `@Pattern` on query parameters, and a `SORTABLE_FIELDS` whitelist that rejects unknown sort fields with HTTP 400.

#### Decisions and Modifications

**LIKE-based search, not full-text search.**

For 10,000 employees on an indexed table, `LOWER(col) LIKE '%term%'` was considered sufficient without requiring a `tsvector` column or dictionary configuration. Documented as Decision 15.

**Sort whitelist enforced at the controller.**

Unknown sort fields raise `IllegalArgumentException`, mapped to HTTP 400.

**Page size capped at 100.**

Requests exceeding the limit return HTTP 400.

**Empty criteria handling.**

`fromCriteria` returns `null` when criteria are empty. The service checks for `null` and falls back to `findAll(pageable)`.

**Repository specification support.**

`EmployeeRepository` now extends `JpaSpecificationExecutor`.

### Part 2 — Exception Mapping Refinement

Two controller tests expected HTTP 400 for an unknown sort field and an invalid enum value, but the handler returned HTTP 409. This exposed a semantic problem: both `IllegalArgumentException` and the duplicate-employee business rule were mapped to the same status.

#### Decisions and Modifications

- Introduced `DuplicateEmployeeException`, mapped to HTTP 409.
- Mapped `IllegalArgumentException` to HTTP 400.
- Added a dedicated `MethodArgumentTypeMismatchException` handler.
- Added a dedicated `ConstraintViolationException` handler.

The resulting status mapping is:

| HTTP status | Meaning |
|---|---|
| 400 Bad Request | Invalid input or request parameters |
| 404 Not Found | Requested record does not exist |
| 409 Conflict | Duplicate or conflicting data |

Documented as Decision 18.

### Part 3 — Test Infrastructure and Configuration

Two controller tests initially failed with HTTP 401 for all requests. The cause was that `@Validated` on the controller caused Spring Security's filter chain to reject MockMvc requests before the controller ran.

#### Decisions and Modifications

**Controller test configuration**

- Added `@AutoConfigureMockMvc(addFilters = false)` to `EmployeeControllerTest`. Documented as Decision 17.
- Removed `.with(csrf())` from POST tests and removed `@WithMockUser`.
- Retained `@Import({GlobalExceptionHandler.class, MethodValidationPostProcessor.class})`.

**Reproducible local environment**

The previous workflow of setting `$env:DB_*` variables per PowerShell session and toggling the development profile in IntelliJ was error-prone.

The revised configuration uses:

- `backend/.env` for local environment variables. The file is Git-ignored.
- `SPRING_PROFILES_ACTIVE=dev` in that file.
- `me.paulschwarz:springboot4-dotenv` with the `spring-dotenv-bom` BOM. The earlier `spring-dotenv:4.0.0` artifact targets Spring Boot 3 and does not work under Spring Boot 4.
- Double quotes around password values in `.env` when they contain `#` or `$`. Documented as Decision 16.
- `application-dev.properties` for development-specific property overrides.

Removed `spring.profiles.active=dev` from `application.properties` and replaced it with external profile selection through `SPRING_PROFILES_ACTIVE=dev`.

**Test profile precedence**

Verified that `@ActiveProfiles("test")` still takes effect even when `SPRING_PROFILES_ACTIVE=dev` is defined in `.env`.

### Verification

Ran the full test suite:

```powershell
.\mvnw.cmd clean test
```

**Recorded result at the time:**

```text
Tests run: 33, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

| Test class | Tests |
|---|---:|
| `EmployeeControllerTest` | 10 |
| `EmployeeRepositoryTest` | 5 |
| `EmployeeServiceTest` | 8 |
| `EmployeeSpecificationsTest` | 9 |
| `SalaryManagementApplicationTests` | 1 |
| **Total** | **33** |

### Manual API Verification

Started the application using `mvnw spring-boot:run` from a fresh PowerShell window, without `$env:` exports or IntelliJ run-configuration overrides.

The startup log confirmed:

```text
The following 1 profile is active: "dev"
HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection@...
Tomcat started on port 8080
```

Postman requests against the running instance:

| # | Request | Expected result | Recorded result |
|---:|---|---|---|
| 1 | `GET /api/employees?search=ada` | HTTP 200 with matching page | Matched |
| 2 | `GET /api/employees?country=GB` | HTTP 200 with filtered page | Matched |
| 3 | `GET /api/employees?sort=lastName,asc&size=5` | HTTP 200 with sorted page | Matched |
| 4 | `GET /api/employees?sort=passwordHash,asc` | HTTP 400 with unsupported-sort-field message | Matched |
| 5 | `GET /api/employees?status=BANANA` | HTTP 400 with invalid-enum message | Matched |
| 6 | `GET /api/employees?size=500` | HTTP 400 with size-cap message | Matched |
| 7 | `GET /api/employees?country=GBR` | HTTP 400 with country-format message | Matched |

### Commit

`feat(backend): add employee search, filters, sort whitelist, and reproducible local configuration`

---

## 2025-10-20 — Salary Records Module: Migration, Entity, Service, Controller, and Tests

The salary milestone adds append-only salary records to the employee module, resolves the current salary as the latest effective record on or before today, and validates currency codes against ISO 4217.

It also surfaced two real problems that were resolved through disciplined engineering rather than shortcuts: a schema type mismatch between the migration and the entity, and the correct choice of dotenv library under Spring Boot 4.

### Part 1 — Design and Schema

#### Prompt

> Design the salary records module: append-only history, ISO 4217 currency, effective date, current-salary resolution by latest effective date on or before today, salary endpoints nested under the employee. Add Flyway migration, entity, repository, service, controller, and tests.

#### AI Output

Suggested:

- `V2__create_salary_records.sql` with an ID, employee foreign key, `NUMERIC(15,2)` amount, currency code, effective date, and creation timestamp.
- A unique constraint on `(employee_id, effective_date)`.
- An index on `(employee_id, effective_date DESC)`.
- A deliberately immutable `SalaryRecord` JPA entity, with no setters for amount, currency code, effective date, or employee.
- `SalaryRecordRepository` with derived queries, including `findTopByEmployeeIdAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc`.
- `SalaryRecordService` with employee-existence checks, amount positivity, a future-date sanity bound of 10 years, currency validation via `java.util.Currency`, duplicate-effective-date detection, a sort whitelist, and a page-size cap.
- `SalaryRecordController` exposing POST, GET salary-history, and GET current-salary endpoints.
- DTOs: `SalaryRecordRequest`, `SalaryRecordResponse`, and `CurrentSalaryResponse`.
- New exceptions: `DuplicateSalaryException` (409) and `InvalidCurrencyException` (400).
- Three new test classes: `SalaryRecordServiceTest`, `SalaryRecordRepositoryTest`, and `SalaryRecordControllerTest`.

#### Decisions and Modifications

**Current-salary resolution**

Chose Option A: the current salary is the latest record whose `effectiveDate` is on or before today. Future-dated records appear in history but not in current-salary results.

Rejected "latest `created_at` wins" and "no future dating" as inferior for a real HR Manager workflow. Documented as Decision 19.

**Currency validation**

Chose `java.util.Currency` rather than a hand-written whitelist. The JDK supplies ISO 4217 currency data, and `Currency.getInstance(code)` throws for unknown codes without requiring an additional dependency.

**Money representation**

Amounts use `BigDecimal`, not `double`. The database stores amounts as `NUMERIC(15,2)`.

**Effective-date representation**

Uses `LocalDate`, not `TIMESTAMPTZ`, because salary changes take effect on a calendar day.

**Referential integrity**

Uses `ON DELETE RESTRICT` on the employee foreign key to prevent deleting employees who have salary history.

**Service-level validation**

The salary service also enforces the page-size cap and sort whitelist, providing a safety net for callers that bypass the controller.

### Part 2 — Schema Type Mismatch: `CHAR(3)` vs `VARCHAR(3)`

After all files were saved, `mvnw clean test` failed on 21 tests with:

```text
Schema validation: wrong column type encountered in column [currency_code]
in table [salary_records]; found [bpchar (Types#CHAR)], but expecting [varchar(3) (Types#VARCHAR)]
```

**Root cause**

V2 declared `currency_code CHAR(3)`. PostgreSQL stores `CHAR(n)` as fixed-width `bpchar`. Hibernate's `ddl-auto=validate` mode compares the entity's declared mapping (`String` with `@Column(length = 3)`, corresponding to `VARCHAR(3)`) against the actual column (`bpchar`) and rejects the mismatch at startup.

#### Decisions and Modifications

**Fixed the schema rather than weakening validation.**

The alternatives were changing the entity to `@Column(columnDefinition = "char(3)")` or relaxing `ddl-auto` to `none`. Both were rejected.

`VARCHAR(3)` is the conventional PostgreSQL choice for a variable-length textual code. The existing check constraint, `CHECK (currency_code ~ '^[A-Z]{3}$')`, continues to enforce the expected format.

Hibernate validation caught a real mismatch. Weakening it would trade a correctness check for convenience.

**Added a new migration instead of editing V2.**

V2 had already been applied to the local database. Editing an applied migration could break other environments or require manual Flyway history-table changes.

This follows Decision 4: never edit an applied migration.

V3, `V3__alter_salary_records_currency_code.sql`, changes the column type:

```sql
ALTER TABLE salary_records
ALTER COLUMN currency_code TYPE VARCHAR(3)
USING currency_code::varchar(3);
```

The currency-format check remains in force.

Documented as Decision 20.

### Verification

After V3 was added, the `mvnw clean test` output included:

```text
Successfully validated 3 migrations
Current version of schema "public": 2
Migrating schema "public" to version "3 - alter salary records currency code"
Successfully applied 1 migration to schema "public", now at version v3
```

Hibernate's validation then passed and all tests ran to completion.

Confirmed in pgAdmin that `information_schema.columns` reported `currency_code` as `character varying` with length `3`.

### Part 3 — Test Coverage

Three new test classes were added.

**`SalaryRecordServiceTest` — 12 tests**

- Record creation: employee missing, invalid currency, future-date bound, duplicate effective date, and currency normalisation.
- Current salary: employee missing, no effective record, and latest effective record.
- Listing: employee missing, mapped page, unsupported sort field, and page-size cap.

**`SalaryRecordRepositoryTest` — 6 tests**

- Save and find by employee ID.
- Unique constraint on `(employee_id, effective_date)`.
- Non-positive amount rejected.
- Current salary resolves to the latest effective record on or before today.
- Current salary returns empty when only future-dated records exist.
- Salary history is sorted by effective date descending.

**`SalaryRecordControllerTest` — 10 tests**

- Record creation returns 201.
- Invalid currency returns 400.
- Negative amount returns 400.
- Duplicate effective date returns 409.
- Missing employee returns 404.
- Current salary returns 200.
- Missing current salary returns 404.
- Salary history returns 200.
- Oversized page size returns 400.
- Unsupported sort field returns 400.

All three follow the patterns established in the employee milestone:

- `@AutoConfigureMockMvc(addFilters = false)` on the controller test.
- `@Transactional` and UUID-based employee numbers on the repository test.
- A mocked repository on the service test.

### Verification

Ran the full test suite:

```powershell
.\mvnw.cmd clean test
```

**Recorded result at the time:**

```text
Tests run: 61, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

| Test class | Tests |
|---|---:|
| `EmployeeControllerTest` | 10 |
| `EmployeeRepositoryTest` | 5 |
| `EmployeeServiceTest` | 8 |
| `EmployeeSpecificationsTest` | 9 |
| `SalaryRecordControllerTest` | 10 |
| `SalaryRecordRepositoryTest` | 6 |
| `SalaryRecordServiceTest` | 12 |
| `SalaryManagementApplicationTests` | 1 |
| **Total** | **61** |

### Manual API Verification

Started the application using `mvnw spring-boot:run` from PowerShell, with the development profile active through `.env`.

| # | Request | Expected result | Recorded result |
|---:|---|---|---|
| 1 | `POST /api/employees` with `EMP-SALARY-01` | HTTP 201 with generated ID | Matched |
| 2 | `POST .../salaries` with amount `50000`, currency `GBP`, and effective date `2025-01-01` | HTTP 201 | Matched |
| 3 | `POST .../salaries` with amount `55000`, currency `GBP`, and effective date `2026-01-01` | HTTP 201 | Matched |
| 4 | `POST .../salaries` with amount `60000`, currency `GBP`, and effective date `2030-01-01` | HTTP 201 | Matched |
| 5 | `GET .../salaries/current` | HTTP 200, returns the `2026-01-01` record | Matched |
| 6 | `GET .../salaries` | HTTP 200, three records ordered 2030 → 2026 → 2025 | Matched |
| 7 | Duplicate salary POST with effective date `2025-01-01` | HTTP 409 with duplicate message | Matched |
| 8 | Salary POST with currency `XYZ` | HTTP 400 with ISO 4217 error message | Matched |
| 9 | `GET .../salaries/current` for employee `999999` | HTTP 404 | Matched |
| 10 | `GET .../salaries?size=500` | HTTP 400 with page-size cap message | Matched |
| 11 | `GET /api/employees/{id}` | HTTP 200, employee still exists | Matched |

Also verified in pgAdmin that:

- Three rows existed in `salary_records` for the test employee with the expected effective-date ordering.
- `information_schema.columns` reported `currency_code` as `character varying` with length `3`, confirming V3 was applied.

The Postman collection was re-exported with all eleven scenarios and committed at `docs/postman/acme-salary-management.postman_collection.json`.

### Key Engineering Notes

Two problems surfaced during this milestone:

1. The `CHAR(3)` versus `VARCHAR(3)` schema mismatch.
2. The incompatibility between the earlier `spring-dotenv:4.0.0` dependency and Spring Boot 4.

Both were diagnosed from actual output and fixed through the appropriate mechanism: a new migration and a corrected dependency. Neither was papered over by weakening validation.

These decisions are documented in the commit history and `docs/design-decisions.md`, demonstrating engineering judgment beyond simply getting the code to compile.

### Commit

`feat(backend): add salary records module with append-only history, currency validation, and 28 new tests`

---

## 2025-10-21 — Reporting Module: Per-Currency Aggregates and a ClassCastException Bug

The reporting module adds dashboard, per-country, per-department, distribution, and top-earners endpoints. All aggregations are per-currency and use PostgreSQL-specific SQL.

This milestone surfaced a production bug: a `ClassCastException` caused by a JDBC type mismatch. It exposed a gap in the test strategy and motivated a new class of tests.

### Part 1 — Design of the Reporting Module

#### Prompt

> Design the reporting module: dashboard headline stats, salary aggregates by country and by department, a salary distribution histogram, and a top-earners list. All aggregations must be per-currency; there must be no cross-currency total. Use native SQL where JPA would be less efficient or impossible. Add service, controller, repository, DTOs, and tests.

#### AI Output

Suggested:

- `ReportController` with five endpoints: `/api/reports/dashboard`, `/salary-by-country`, `/salary-by-department`, `/salary-distribution`, and `/top-earners`.
- `ReportRepository` with seven native SQL queries. Every query uses the `current_salaries` CTE pattern from Decision 19.
- `ReportService` validating inputs, bounding numeric parameters, and mapping `Object[]` results to typed DTOs.
- DTOs: `CurrencyTotal`, `DashboardResponse`, `DistributionBucket`, `DistributionResponse`, `SalaryAggregateResponse`, `SalaryAggregateRow`, `TopEarnerRow`, and `TopEarnersResponse`.
- Three new test classes: `ReportControllerTest`, `ReportRepositoryTest`, and `ReportServiceTest`.

#### Decisions and Modifications

**Native SQL for aggregations**

The queries use `DISTINCT ON (employee_id)` to select the current salary per employee and `width_bucket(...)` for histograms. Neither has a direct JPQL equivalent, and a Java-side rewrite of `width_bucket` would be slower and harder to test.

Documented as Decision 21.

**Per-currency scope on every aggregate**

Salary-by-country, salary-by-department, and top-earners require a currency parameter. The dashboard returns `salaryTotalsByCurrency` as an array.

There is deliberately no `total payroll` field. See Decision 10.

**Bounded numeric parameters**

- `@Max(100)` on the top-earners limit.
- `@Max(200)` on the distribution bucket count.

These bounds limit the cost of individual requests.

**Required parameters**

A missing required `@RequestParam` triggers `MissingServletRequestParameterException`. This was initially not handled by `GlobalExceptionHandler`; see Part 3.

**Distribution validation**

`bucketSize` is validated in the service. It must be at least `1.00` and must produce no more than 200 buckets for the current maximum salary. Invalid values are rejected as `IllegalArgumentException`, mapped to HTTP 400.

### Part 2 — Cast Syntax Bug in Native Query

After the first save, `ReportRepositoryTest` failed on the distribution query with a Hibernate named-parameter error. The query contained:

```sql
cast(? as integer)
```

Hibernate 7's named-parameter parser treated `? as integer` as part of a malformed parameter reference.

#### Decisions and Modifications

- Replaced positional `?` syntax with named parameters. `cast(:bucketCount as integer)` binds correctly because the parser terminates the parameter name at `as`.
- Applied `cast(... as numeric)` to every parameter in the `width_bucket` call. Without explicit casts, PostgreSQL's overload resolution may select the wrong overload or fail to resolve one.

Documented as an inline note in `ReportRepository.salaryDistribution`.

### Part 3 — Missing-Parameter Handler Gap

`ReportControllerTest.salaryByCountryRejectsMissingCurrency` passed, but only because Spring's default `MissingServletRequestParameterException` handler also returns HTTP 400. The response body, however, had a different structure from `ApiErrorResponse`.

#### Decisions and Modifications

- Added `MissingServletRequestParameterException` handling to `GlobalExceptionHandler`.
- Standardised the response shape for HTTP 400 errors.
- Added a test asserting the response structure: `salaryByCountryReturnsConsistentErrorBodyForMissingCurrency`.

### Part 4 — The `ClassCastException` Bug

All 105 tests passed, but manual Postman testing of `/api/reports/top-earners?currency=GBP&limit=5` returned HTTP 500. The application log showed:

```text
java.lang.ClassCastException: class java.time.LocalDate cannot be cast to class java.sql.Date
    at za.co.acme.salarymanagement.report.ReportService.topEarners(ReportService.java:168)
```

**Root cause**

`ReportService.topEarners` cast the `effective_date` column to `java.sql.Date`. PostgreSQL 17.6 with Hibernate 7.4.5 returns `java.time.LocalDate` for a `DATE` column materialised in a native `Object[]` result. The cast therefore failed at runtime.

The mock-based unit test `ReportServiceTest.topEarnersMapsRows` supplied `java.sql.Date`, a type that did not match the actual runtime result. The test passed while the application failed.

This demonstrated a limitation of mock-based testing for native query results.

#### Decisions and Modifications

**Normalised date conversion**

Added a `toLocalDate(Object)` helper that accepts both `java.time.LocalDate` and `java.sql.Date`, and rejects unexpected types with an explicit `IllegalStateException`.

Returning `null` silently could corrupt the top-earners display, so unexpected types must fail clearly.

**Corrected the unit test**

Changed `ReportServiceTest.topEarnersMapsRows` to use `LocalDate`, matching the type returned in the production environment. Renamed it to `topEarnersMapsRowsWithLocalDateFromHibernate` to make the intent explicit.

**Added tests for accepted and rejected types**

Added:

- `topEarnersAcceptsSqlDateFromAlternativeDrivers`
- `topEarnersRejectsUnexpectedDateType`

These verify compatibility with the alternative JDBC type and the rejection of unexpected values.

**Added a real PostgreSQL integration test**

Added `ReportServiceIntegrationTest` to exercise the complete native-query-to-DTO mapping path against real PostgreSQL. It uses the distinctive currency code `XDR` to isolate its data from other records in the shared test database.

Documented as Decision 22.

### Verification

Ran the full test suite:

```powershell
.\mvnw.cmd clean test
```

**Recorded result at the time:**

```text
Tests run: 105, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

| Test class | Tests |
|---|---:|
| `EmployeeControllerTest` | 10 |
| `EmployeeRepositoryTest` | 5 |
| `EmployeeServiceTest` | 8 |
| `EmployeeSpecificationsTest` | 9 |
| `ReportControllerTest` | 12 |
| `ReportRepositoryTest` | 10 |
| `ReportServiceIntegrationTest` | 5 |
| `ReportServiceTest` | 17 |
| `SalaryRecordControllerTest` | 10 |
| `SalaryRecordRepositoryTest` | 6 |
| `SalaryRecordServiceTest` | 12 |
| `SalaryManagementApplicationTests` | 1 |
| **Total** | **105** |

### Manual API Verification

Started the application using `mvnw spring-boot:run`. All 25 Postman requests against the running instance returned the expected status codes.

| # | Request | Expected result | Recorded result |
|---:|---|---|---|
| 1–17 | Employee and salary smoke tests, previously verified | As documented | Matched |
| 18 | `GET /api/reports/dashboard` | HTTP 200 with headline counts and per-currency totals | Matched |
| 19 | `GET /api/reports/salary-by-country?currency=GBP` | HTTP 200 with `groupingDimension: "country"` | Matched |
| 20 | `GET /api/reports/salary-by-department?currency=GBP` | HTTP 200 with `groupingDimension: "department"` | Matched |
| 21 | `GET /api/reports/salary-by-country?currency=XYZ` | HTTP 400 with `Unknown ISO 4217 currency code: XYZ` | Matched |
| 22 | `GET /api/reports/salary-distribution?currency=GBP&bucketSize=10000` | HTTP 200 with a buckets array | Matched |
| 23 | `GET /api/reports/top-earners?currency=GBP&limit=5` | HTTP 200 with an earners array; previously returned HTTP 500 | Matched after fix |
| 24 | `GET /api/reports/top-earners?currency=GBP&limit=500` | HTTP 400 with the `@Max(100)` message | Matched |
| 25 | `GET /api/reports/salary-by-country` without currency | HTTP 400 with a required-parameter message | Matched |

### Key Engineering Notes

The `ClassCastException` is the most important entry in this work log. It was a real bug that reached manual testing despite 98 unit and repository tests passing.

The lesson recorded in Decision 22 is:

> Mock-based unit tests verify that code behaves as expected for a particular input. They cannot verify that the runtime will actually produce that input. For native queries returning `Object[]`, the DTO mapping layer must be covered by integration tests against the real driver.

The engineering response was to:

1. Fix the production code defensively by accepting both supported date types and rejecting others.
2. Update the mock-based test to use the type production actually returns.
3. Add explicit tests for each accepted type and for the rejection path.
4. Add an integration test covering the complete path against real PostgreSQL.
5. Document the rule so future native queries follow the same pattern.

This demonstrates engineering judgment beyond simply writing code that compiles.

### Commit

`fix(report): normalise DATE type from Hibernate native query in top-earners; add reporting module`

---

## Remaining Verification Work

The following items remain outstanding and should be updated as implementation progresses:

- [ ] CSV export of filtered employee and salary results.
- [ ] JWT authentication and endpoint protection, replacing `DevSecurityConfig`.
- [ ] Dedicated security test class covering unauthenticated and unauthorised requests.
- [ ] Angular frontend.
- [ ] Deterministic seed-data generator for 10,000 employees.
- [ ] Dockerfiles and `docker-compose.yml`.
- [ ] Deployment and demonstration video.

## Record-Keeping Rule

Never record a test, feature, or security control as verified unless the relevant check has actually been run and its result is available.

Historical results must remain distinguishable from current results. Update this work log whenever new implementation decisions, AI-assisted changes, verification results, or related commits are made.
