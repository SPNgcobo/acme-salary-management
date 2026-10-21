# ACME Salary Management System: Design Decisions

This document records the key technical decisions for the ACME Salary Management System, including their rationale, trade-offs, implementation status, and verification evidence.

A decision may be **Accepted** while its implementation remains pending.

## Table of Contents

- [Status Legend](#status-legend)
- [Decision 1: Modular Monolith](#decision-1-modular-monolith)
- [Decision 2: Java 21 and Spring Boot 4.1.1](#decision-2-java-21-and-spring-boot-411)
- [Decision 3: PostgreSQL](#decision-3-postgresql)
- [Decision 4: Flyway for Database Migrations](#decision-4-flyway-for-database-migrations)
- [Decision 5: Controller, Service, and Repository Layers](#decision-5-controller-service-and-repository-layers)
- [Decision 6: Validate at Multiple Levels](#decision-6-validate-at-multiple-levels)
- [Decision 7: Consistent API Error Responses](#decision-7-consistent-api-error-responses)
- [Decision 8: Server-Side Search and Pagination](#decision-8-server-side-search-and-pagination)
- [Decision 9: Separate Employee Profiles from Salary History](#decision-9-separate-employee-profiles-from-salary-history)
- [Decision 10: Handle Currencies Explicitly](#decision-10-handle-currencies-explicitly)
- [Decision 11: Keep Credentials Outside Source Control](#decision-11-keep-credentials-outside-source-control)
- [Decision 12: Automated Tests Against Real PostgreSQL](#decision-12-automated-tests-against-real-postgresql)
- [Decision 13: Protect Sensitive Data](#decision-13-protect-sensitive-data)
- [Decision 14: Containerise with Docker](#decision-14-containerise-with-docker)
- [Decision 15: Use LIKE-Based Search for Version 1](#decision-15-use-like-based-search-for-version-1)
- [Decision 16: Load Environment Variables Using springboot4-dotenv](#decision-16-load-environment-variables-using-springboot4-dotenv)
- [Decision 17: Disable Security Filters in Controller-Focused Tests](#decision-17-disable-security-filters-in-controller-focused-tests)
- [Decision 18: Use Semantically Correct HTTP Status Codes](#decision-18-use-semantically-correct-http-status-codes)
- [Decision 19: Resolve Current Salary by Effective Date](#decision-19-resolve-current-salary-by-effective-date)
- [Decision 20: Use VARCHAR(3) for Currency Codes](#decision-20-use-varchar3-for-currency-codes)
- [Decision 21: Use Native SQL for Reporting Aggregations](#decision-21-use-native-sql-for-reporting-aggregations)
- [Decision 22: Integration Tests for Native-Query DTO Mapping](#decision-22-integration-tests-for-native-query-dto-mapping)
- [Maintaining This Document](#maintaining-this-document)

---

## Status Legend

| Status | Meaning |
|---|---|
| **Accepted** | The technical decision has been agreed upon. |
| **Implemented** | The corresponding code or configuration exists. |
| **Verified** | Relevant automated tests or checks have passed. |
| **Pending** | Implementation has not started or is not yet complete. |
| **In Progress** | Implementation or verification remains incomplete. |

---

## Decision 1: Modular Monolith

**Status:** Accepted; Implemented

### Decision

Build one Spring Boot application divided into employee, salary, reporting, security, and common modules.

### Reason

The stated scope does not justify the operational complexity of microservices. A single deployable application is simpler to test, run, and demonstrate.

### Trade-off

Module boundaries must be maintained intentionally to prevent tightly coupled code and an increasingly tangled monolith.

### Implementation

The `common`, `employee`, `salary`, and `report` packages exist. The dedicated `security` package remains planned.

---

## Decision 2: Java 21 and Spring Boot 4.1.1

**Status:** Accepted; Implemented

### Decision

Use Java 21 LTS and Spring Boot 4.1.1 for the backend.

### Reason

Java 21 provides an LTS toolchain, while Spring Boot offers an established ecosystem for REST APIs, validation, dependency injection, persistence, and testing.

Spring Boot 4 provides a modern framework baseline and updated security and testing support.

### Trade-off

Spring Boot 4 modularised some test starters and moved certain annotations into new packages. For example, `@DataJpaTest` is available through:

```java
org.springframework.boot.data.jpa.test.autoconfigure
```

This introduced migration work, but the current framework baseline was retained.

### Implementation

Recorded as implemented. The application is configured for Spring Boot 4.1.1, and imports were adjusted for the updated package structure.

---

## Decision 3: PostgreSQL

**Status:** Accepted; Implemented

### Decision

Use PostgreSQL 17 as the relational database.

### Reason

PostgreSQL provides relational integrity, constraints, transactions, and analytical SQL capabilities for reporting. Managed hosting options, such as Neon, also support PostgreSQL in development and deployment.

### Trade-off

A database server must be running and accessible, unlike an embedded file-based database. This was accepted to provide realistic database behaviour.

### Implementation

The database connection was verified through HikariCP, and Flyway migrations were applied against `acme_salary_management` using PostgreSQL 17.6, according to the recorded development results.

### Least-Privilege Database Access

The application connects using a dedicated `acme_app` role rather than the PostgreSQL superuser. Permissions are scoped to the application's database and schema requirements.

---

## Decision 4: Flyway for Database Migrations

**Status:** Accepted; Implemented

### Decision

Use versioned SQL migrations managed by Flyway.

### Reason

Schema changes become repeatable, reviewable, and traceable across development and deployment environments.

### Rule

**Never modify an applied migration. Introduce schema changes through a new migration version.**

### Trade-off

Flyway requires more explicit migration management than `spring.jpa.hibernate.ddl-auto=update`, but it provides a safer and more controlled schema-evolution process.

### Implementation

The following migrations are recorded as implemented:

- `V1__create_employees.sql`
- `V2__create_salary_records.sql`
- `V3__alter_salary_records_currency_code.sql`

V3 corrected the currency column type introduced in V2 after Hibernate schema validation identified a mismatch. The correction was made through a new migration rather than by modifying V2.

Hibernate uses `ddl-auto=validate` to detect inconsistencies between entity mappings and the database schema.

---

## Decision 5: Controller, Service, and Repository Layers

**Status:** Accepted; Implemented

### Decision

Separate responsibilities across three application layers:

- **Controller:** Handles HTTP requests and responses.
- **Service:** Implements business rules.
- **Repository:** Handles persistence and database queries.

### Reason

Clear separation makes the code easier to test, maintain, and reason about.

### Trade-off

The design introduces more classes and indirection. This is justified because salary history, currency validation, and employee management involve business rules that should not be embedded in HTTP controllers.

### Implementation

The employee, salary, and report modules follow this pattern.

| Layer | Components | Responsibility |
|---|---|---|
| Controller | `EmployeeController`, `SalaryRecordController`, `ReportController` | HTTP concerns, parameter validation, and sort-field whitelisting. |
| Service | `EmployeeService`, `SalaryRecordService`, `ReportService` | Business rules and application operations. |
| Repository | `EmployeeRepository`, `SalaryRecordRepository`, `ReportRepository` | Persistence and database queries. |

---

## Decision 6: Validate at Multiple Levels

**Status:** Accepted; Implemented

### Decision

Use Jakarta Bean Validation for request validation and database constraints for data integrity.

### Reason

Request validation provides useful client-facing errors. Database constraints protect data integrity even when concurrent requests attempt conflicting operations.

### Trade-off

Rules enforced at both application and database levels must remain consistent.

### Implementation

Request DTOs use annotations including:

- `@NotBlank`
- `@Email`
- `@Pattern`
- `@NotNull`
- `@DecimalMin`
- `@Digits`
- `@Size`

Controllers use method-level constraints such as `@Min`, `@Max`, `@Size`, and `@Pattern`, activated by `@Validated`.

Database `CHECK` and `UNIQUE` constraints provide additional protection.

`GlobalExceptionHandler` maps request-validation and method-validation failures to HTTP 400.

Salary-specific rules, including positive amounts, effective-date limits, currency validity, and duplicate effective dates, are enforced by the service and mapped to appropriate HTTP status codes.

---

## Decision 7: Consistent API Error Responses

**Status:** Accepted; Implemented

### Decision

Use a shared API error response structure through a global exception handler.

### Reason

Clients need predictable error formats for validation failures, missing resources, conflicts, and other expected errors.

### Trade-off

A shared structure may be more formal than necessary for trivial cases, but consistency simplifies client-side error handling.

### Implementation

`GlobalExceptionHandler` returns `ApiErrorResponse` for handled validation, type-mismatch, missing-parameter, illegal-argument, invalid-currency, duplicate, and not-found cases.

The intended HTTP status mapping is:

| HTTP status | Meaning | Examples |
|---|---|---|
| `400 Bad Request` | Invalid request or parameter | Invalid enum, unsupported sort field, oversized page size, invalid currency, missing required parameter. |
| `404 Not Found` | Requested resource does not exist | Employee ID not found. |
| `409 Conflict` | Request conflicts with existing state | Duplicate employee number or email; duplicate salary effective date. |

A `MissingServletRequestParameterException` handler was added during the reporting milestone to close a consistency gap. Previously, missing required `@RequestParam` values fell through to Spring's default handler, producing a response body shaped differently from `ApiErrorResponse`.

All HTTP 400 responses handled by this exception handler now use the shared response structure.

---

## Decision 8: Server-Side Search, Filtering, Pagination, and Sorting

**Status:** Accepted; Implemented

### Decision

Perform pagination, sorting, search, and filtering at the database query level using Spring Data JPA `Specification` and `Pageable`.

### Reason

The system must support approximately 10,000 employees without loading every record into application memory or sending the entire directory to the browser.

### Trade-off

Dynamic query construction introduces complexity, but it supports scalability and a more usable employee directory.

### Implementation

`EmployeeSpecifications` composes specifications for the supported search and filters.

Both employee and salary controllers validate pagination and sorting parameters against the relevant whitelists and limits before passing a `Pageable` to the service.

---

## Decision 9: Separate Employee Profiles from Salary History

**Status:** Accepted; Implemented

### Decision

Store salary records separately from employee profiles and treat salary history as append-only. Each salary record has an effective date and currency.

Previous salary records must not be overwritten.

### Reason

Salary history needs to remain auditable. Updating previous records would destroy the record of earlier salary arrangements.

### Trade-off

The salary table grows over time. Pagination and appropriate indexes support querying the history.

### Implementation

The append-only design is implemented at three levels:

1. **Database:** `UNIQUE (employee_id, effective_date)` prevents duplicate effective dates for an employee. `ON DELETE RESTRICT` prevents deleting an employee while salary records reference that employee.
2. **Entity:** `SalaryRecord` does not expose setters for `amount`, `currencyCode`, `effectiveDate`, or `employee`.
3. **Service:** `SalaryRecordService` has no update or delete methods. Salary changes are recorded as new inserts.

---

## Decision 10: Handle Currencies Explicitly

**Status:** Accepted; Implemented

### Decision

Store an ISO 4217 currency code with each salary record. Reports must group amounts by currency and must not combine different currencies without a documented conversion methodology.

### Reason

Adding amounts expressed in different currencies produces misleading results.

### Trade-off

Cross-currency totals are deferred. The system provides meaningful per-currency values rather than an incorrect combined total.

### Implementation

The `currency_code` column uses `VARCHAR(3)` and a database constraint requiring three uppercase letters:

```sql
CHECK (currency_code ~ '^[A-Z]{3}$')
```

The service normalises currency codes to uppercase and validates them using `java.util.Currency.getInstance(...)`. Unknown currency codes cause `InvalidCurrencyException`, which maps to HTTP 400.

The reporting endpoints are scoped to a single currency per request. The dashboard exposes `salaryTotalsByCurrency` as an array, with one entry per currency present in the current salary data.

There is deliberately no single `totalPayroll` field. A client that wants a total must choose a currency or apply an FX rule.

`TopEarnersResponse` is similarly per-currency. Ranking a 50,000 GBP salary against a 60,000 USD salary without an FX rule would be misleading.

---

## Decision 11: Keep Credentials Outside Source Control

**Status:** Accepted; Implemented

### Decision

Supply secrets through environment variables loaded from a local `.env` file. Keep `.env` Git-ignored and commit `.env.example` with placeholder values.

### Reason

This reduces the risk of accidentally committing credentials and follows common configuration-management practices.

### Trade-off

Each developer must configure their own local environment.

### Implementation

- The local environment file is `backend/.env`.
- `backend/.env` is Git-ignored, with the ignore rule checked using `git check-ignore`.
- `.env.example` is committed at the repository root and contains placeholder values.
- The environment-loading mechanism is documented in [Decision 16](#decision-16-load-environment-variables-using-springboot4-dotenv).

---

## Decision 12: Automated Tests Against Real PostgreSQL

**Status:** Accepted; Implemented

### Decision

Use JUnit 5 and Spring Boot Test to test business rules and API behaviour. Run repository and specification tests against PostgreSQL rather than an embedded H2 database.

Repository tests use:

```java
@AutoConfigureTestDatabase(replace = Replace.NONE)
```

### Reason

Flyway migrations, `TIMESTAMPTZ`, `BIGSERIAL`, and database constraints must be tested against the target database engine. H2 may behave differently and could hide database-specific issues.

### Trade-off

Tests require a running PostgreSQL database. CI will need a managed database or an alternative such as Testcontainers.

PostgreSQL-backed tests may take longer than embedded-database tests. This trade-off was accepted because data correctness is particularly important for salaries and employment history.

### Implementation

The recorded test result is 105 passing tests across twelve test classes.

| Test class | Recorded tests |
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

The reporting milestone added 44 tests across the reporting controller, repository, service, and integration test classes.

> **Verification note:** Confirm this count by running the complete test suite before submission.

### Data isolation

`EmployeeRepositoryTest`, `EmployeeSpecificationsTest`, `SalaryRecordRepositoryTest`, `ReportRepositoryTest`, and `ReportServiceIntegrationTest` use `@Transactional` for rollback after each test and generate unique identifiers, or use a distinctive currency code in the case of `ReportServiceIntegrationTest`, to reduce collisions with data created through Postman or previous test runs.

### Follow-up

Introduce Testcontainers so CI can provision a reproducible PostgreSQL environment without relying on a manually configured database.

---

## Decision 13: Protect Sensitive Data

**Status:** Accepted; In Progress

### Decision

Use Spring Security with JWT authentication and authorisation to protect employee and salary endpoints. Use synthetic data for development and demonstrations.

### Reason

Salary information is confidential. Unauthenticated write access is unacceptable for production.

### Trade-off

Authentication introduces login and token-management complexity, but these are necessary for a realistic application.

### Implementation

Pending. The current development configuration uses `DevSecurityConfig`, guarded by `@Profile("dev")`, to disable CSRF and permit requests for local Postman testing.

Production-ready JWT authentication, authorisation, and security verification remain future work.

---

## Decision 14: Containerise with Docker

**Status:** Accepted; Pending

### Decision

Provide Dockerfiles for the backend and frontend, plus a `docker-compose.yml` that runs PostgreSQL, the backend, and the frontend.

### Reason

Containerisation supports a reproducible setup and a clearer deployment process. It reduces environment differences between development and assessment environments.

### Trade-off

Docker adds configuration and image-build maintenance.

### Implementation

Pending. Containerisation is planned after the core application features.

---

## Decision 15: Use LIKE-Based Search for Version 1

**Status:** Accepted; Implemented

### Decision

Implement free-text employee search using:

```sql
LOWER(column) LIKE '%term%'
```

The search covers `first_name`, `last_name`, `employee_number`, and `email`. Matches across these fields are combined with `OR`, while other filters are combined with the search using `AND`.

PostgreSQL full-text search using `tsvector`, GIN indexes, and dictionary configuration is deferred.

### Reason

For the target dataset of approximately 10,000 employees, LIKE-based search provides a simpler initial implementation.

Full-text search would introduce additional schema and query complexity, including configuration and special handling for employee numbers and email addresses.

### Trade-off

LIKE-based substring searches may not scale efficiently to millions of records and do not provide stemming or relevance ranking.

If measurements show a performance problem, a `pg_trgm` GIN index or a move to `tsvector` can be considered.

### Implementation

Implemented in `EmployeeSpecifications.matchesSearch(String)`.

Recorded test coverage includes:

- `matchesSearchFindsByFirstNameCaseInsensitive`
- `matchesSearchFindsByEmailSubstring`
- `fromCriteriaFindsBySearchAndFilters`

---

## Decision 16: Load Environment Variables Using springboot4-dotenv

**Status:** Accepted; Implemented

### Decision

Load the following local environment variables from `backend/.env` through the `springboot4-dotenv` library from `me.paulschwarz`, using its `spring-dotenv-bom` dependency management:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `JWT_EXPIRATION_MS`
- `SPRING_PROFILES_ACTIVE`

The `.env` file remains Git-ignored. `.env.example` at the repository root is the committed template.

### Reason

Spring Boot does not natively load `.env` files. Without a dotenv integration, developers would need to export variables in each shell session or maintain separate IDE run configurations.

A dotenv library provides a consistent local configuration mechanism for IntelliJ, `mvnw spring-boot:run`, and a packaged JAR, subject to the library's working-directory and loading behaviour.

### Trade-off

A third-party dependency is introduced.

The originally suggested artifact, `me.paulschwarz:spring-dotenv`, targets Spring Boot 3 and did not work with the project's Spring Boot 4 setup. The Spring Boot 4-compatible module and its BOM were selected instead.

A local `.env` file must never be packaged or deployed as a substitute for production configuration. Production secrets and profile selection must come from the hosting environment.

### Additional pitfall

Values containing `#`, `$`, or whitespace may require double quotes according to the dotenv parser's syntax. Incorrect quoting can truncate values and cause confusing configuration errors.

The `.env.example` template documents this behaviour.

### Implementation

Recorded as implemented and verified. The backend was reported to start with the `dev` profile and establish a HikariCP database connection without manual shell-level PowerShell `$env:` exports or IntelliJ-specific environment overrides.

---

## Decision 17: Disable Security Filters in Controller-Focused Tests

**Status:** Accepted; Implemented

### Decision

`EmployeeControllerTest`, `SalaryRecordControllerTest`, and `ReportControllerTest` use `@WebMvcTest` and:

```java
@AutoConfigureMockMvc(addFilters = false)
```

Security filters are not exercised in these controller-focused test classes.

### Reason

After `@Validated` was added to the controllers for method-level parameter validation, Spring Security began rejecting some MockMvc requests with HTTP 401 before the controller logic could execute.

These tests are intended to verify controller behaviour, including parameter binding, status codes, and error responses, rather than authentication.

### Trade-off

These tests do not verify endpoint protection or CSRF behaviour. Those responsibilities must be covered by dedicated security tests.

### Implementation

Implemented according to the recorded test configuration. All three controller test classes pass with security filters disabled.

---

## Decision 18: Use Semantically Correct HTTP Status Codes

**Status:** Accepted; Implemented

### Decision

HTTP status codes must reflect the nature of the failure rather than returning one generic error status.

### Status-code policy

| Status | Use case | Examples |
|---|---|---|
| `400 Bad Request` | Caller-caused input problems | Invalid body, invalid enum, unsupported sort field, non-numeric parameter, oversized page size, invalid currency, missing required parameter. |
| `404 Not Found` | Requested resource does not exist | Employee lookup by unknown ID. |
| `409 Conflict` | Request conflicts with existing state | Duplicate employee number or email; duplicate salary effective date. |

### Reason

Clients need to distinguish invalid input from a conflict with existing data and from a missing resource. A consistent status-code policy improves error handling.

### Trade-off

Different failure categories require explicit exception types and mappings. The additional classes make the intended behaviour clearer.

### Implementation

The recorded implementation includes:

- `EmployeeService.create` throwing `DuplicateEmployeeException` for duplicate employee data.
- `SalaryRecordService.record` throwing `DuplicateSalaryException` and `InvalidCurrencyException` for the corresponding salary failures.
- All three services throwing `EmployeeNotFoundException` when the employee does not exist.
- Controller `parseSort` methods throwing `IllegalArgumentException` for unsupported sort fields, mapped to HTTP 400.

The mappings are covered by controller tests and Postman smoke tests according to the recorded results.

---

## Decision 19: Resolve Current Salary by Effective Date

**Status:** Accepted; Implemented

### Decision

The current salary is the record with the highest `effective_date` on or before today.

Records with a future effective date remain in the salary history but are not treated as current.

### Reason

Salary increases may be scheduled to take effect in the future. The current-salary endpoint should reflect the salary in effect today, rather than simply returning the most recently inserted record.

A future frontend could provide a separate view of upcoming salary changes without changing this backend rule.

### Trade-off

When a future raise is recorded, the current-salary endpoint continues to return the existing salary until the new effective date arrives. The salary-history view should make scheduled changes visible.

### Implementation

`SalaryRecordService.current(...)` uses the repository query:

```java
findTopByEmployeeIdAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
    employeeId,
    LocalDate.now()
)
```

The query is supported by `idx_salary_records_employee_effective_desc`.

The reporting module uses the same rule in SQL: the `current_salaries` CTE with `DISTINCT ON (employee_id) ... ORDER BY effective_date DESC`, combined with `WHERE effective_date <= :onDate`.

Every aggregation query in `ReportRepository` uses this pattern.

### Recorded verification

- `SalaryRecordServiceTest.currentReturnsLatestEffectiveRecord`
- `SalaryRecordRepositoryTest.currentResolvesLatestEffectiveDateOnOrBeforeToday`
- `SalaryRecordRepositoryTest.currentReturnsEmptyWhenOnlyFutureDatedRecords`
- `ReportServiceIntegrationTest.topEarnersIgnoresFutureDatedSalaryRecords`

Postman scenarios also cover past, current, and future salary records and check that the current endpoint returns the record effective today.

---

## Decision 20: Use VARCHAR(3), Not CHAR(3), for Currency Codes

**Status:** Accepted; Implemented

### Decision

The `salary_records.currency_code` column uses `VARCHAR(3)` rather than `CHAR(3)`.

### Reason

The initial V2 migration declared `currency_code` as `CHAR(3)`. PostgreSQL stores this as the fixed-width `bpchar` type, which pads values with trailing spaces.

Hibernate's `ddl-auto=validate` mode detected a mismatch between the database column and the entity's `String` mapping with `@Column(length = 3)`, which expects `VARCHAR(3)`.

Instead of disabling schema validation or adapting the entity to `bpchar`, the schema was corrected to use `VARCHAR(3)`, a conventional choice for variable-length text codes.

The existing database constraint remains in place:

```sql
CHECK (currency_code ~ '^[A-Z]{3}$')
```

### Trade-off

A second migration, `V3__alter_salary_records_currency_code.sql`, was needed because V2 had already been applied.

Editing V2 in place would only have been appropriate if it had not yet been applied to any database that needed to be preserved.

The corrective migration follows the immutable-migration rule in [Decision 4](#decision-4-flyway-for-database-migrations).

### Implementation

V3 changes the column type with:

```sql
ALTER TABLE salary_records
    ALTER COLUMN currency_code TYPE VARCHAR(3)
    USING currency_code::varchar(3);
```

The recorded verification includes a successful Flyway migration, subsequent Hibernate schema validation, and confirmation in pgAdmin that `information_schema.columns` reports `character varying(3)` for the currency column.

---

## Decision 21: Use Native SQL for Reporting Aggregations

**Status:** Accepted; Implemented

### Decision

The reporting module's aggregation queries use native SQL, not JPQL or Criteria API.

### Reason

The queries require PostgreSQL-specific features that have no direct JPA equivalent:

- `DISTINCT ON (employee_id)` selects the current salary per employee in a single pass, using the `idx_salary_records_employee_effective_desc` index.
- `width_bucket(...)` bins salaries into histogram buckets.

A JPQL rewrite would be more verbose, less efficient, and, in the case of `width_bucket`, impossible without custom function registration.

The alternative of loading the current salary for every employee into Java and aggregating in memory was rejected because it would not scale well to 10,000 employees and would defeat the purpose of database-side aggregation.

### Trade-off

Native SQL is coupled to PostgreSQL. If the application is ever ported to a different database, the reporting queries must be rewritten.

This is accepted because the application is explicitly PostgreSQL-based, as documented in Decision 3, and the reporting module is the main place where database-specific features are used.

Native query results also introduce type fragility: `Object[]` results expose JDBC runtime types rather than compile-time types.

The DTO mapping layer is therefore covered by integration tests, as described in [Decision 22](#decision-22-integration-tests-for-native-query-dto-mapping).

### Implementation

`ReportRepository` uses native queries via `@Query(nativeQuery = true)`.

All queries follow the `current_salaries` CTE pattern from Decision 19.

The service is the single place where `Object[]` results are mapped to typed DTOs.

---

## Decision 22: Integration Tests for Native-Query DTO Mapping

**Status:** Accepted; Implemented

### Decision

The reporting service's mapping of native query `Object[]` results to DTOs is covered by `ReportServiceIntegrationTest` against real PostgreSQL, in addition to the mock-based unit tests in `ReportServiceTest`.

### Reason

A real production bug motivated this decision.

`ReportService.topEarners` initially cast the effective-date column to `java.sql.Date`, but PostgreSQL 17.6 with Hibernate 7.4.5 returned `java.time.LocalDate` for a `DATE` column materialised in a native query result.

The cast threw `ClassCastException` at runtime.

The bug was invisible to unit tests because the mock supplied `java.sql.Date`, a type that did not match the actual runtime result.

Mock-based unit tests verify that service code behaves as expected given a particular input. They cannot verify that the database driver will actually produce that input type.

Integration tests wire the real service against the real repository and JDBC driver, closing that gap.

### Trade-off

Integration tests are slower because they require PostgreSQL. The `ReportServiceIntegrationTest` runs five tests and adds approximately two seconds to the suite, according to the recorded results.

This is acceptable given that the bug it prevents would otherwise be caught only during manual Postman testing or in production.

### Implementation

`ReportServiceIntegrationTest` uses:

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("test")
```

The test imports `ReportService` through `@Import`.

Tests seed data inside a rolled-back transaction and use a distinctive currency code (`XDR`) to reduce collisions with other data in the shared test database.

The fix in `ReportService` uses a `toLocalDate(Object)` helper that accepts both `LocalDate` and `java.sql.Date` and rejects other types with an explicit exception.

Three new unit tests in `ReportServiceTest` cover:

1. The `LocalDate` path, matching production behaviour.
2. The `java.sql.Date` compatibility path.
3. The rejection path for unexpected types.

### Rule for future native queries

Any service method that maps native query `Object[]` results to DTOs must have:

- A unit test for each accepted runtime type.
- A unit test for the rejection path when an unexpected type is supplied.
- An integration test that exercises the real database driver.

---

## Maintaining This Document

Update this document when a significant technical decision changes or new evidence affects its status.

- Keep **Accepted**, **Implemented**, and **Verified** separate.
- Do not mark functionality as verified until the relevant checks have passed.
- Introduce new database migrations instead of modifying migrations that have already been applied.
- Keep credentials and other sensitive configuration out of version control.
- Update the related architecture and project-structure documentation when implementation status changes.
- Confirm test counts and verification claims against the latest actual results before submission.

This document records the reasoning behind the ACME Salary Management System's technical choices and provides a traceable record of how those choices are implemented and verified.