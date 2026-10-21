# ACME Salary Management System: Design Decisions

This document records the key technical decisions for the ACME Salary Management System, including the reasoning, trade-offs, implementation status, and verification evidence.

A decision may be **Accepted** while its implementation is still **Pending**.

## Status Legend

| Status | Meaning |
|---|---|
| **Accepted** | The technical decision has been agreed upon. |
| **Implemented** | The corresponding code or configuration exists. |
| **Verified** | Relevant automated tests or checks have passed. |
| **Pending** | Implementation has not started or is not yet complete. |

---

## Decision 1: Modular Monolith

**Status:** Accepted

**Decision**

Build one Spring Boot application divided into employee, salary, reporting, security, and common modules.

**Reason**

The stated scope does not justify the operational complexity of microservices. A single deployable application is simpler to test, run, and demonstrate.

**Trade-off**

Module boundaries must be maintained intentionally to prevent tightly coupled code and an increasingly tangled monolith.

**Implementation**

The `common`, `employee`, and `salary` packages exist. The dedicated `report` and `security` packages remain planned.

## Decision 2: Java 21 and Spring Boot 4.1.1

**Status:** Accepted

**Decision**

Use Java 21 LTS and Spring Boot 4.1.1 for the backend.

**Reason**

Java 21 provides an LTS toolchain, while Spring Boot offers an established ecosystem for REST APIs, validation, dependency injection, persistence, and testing.

Spring Boot 4 provides a modern framework baseline and updated security and testing support.

**Trade-off**

Spring Boot 4 modularised some test starters and moved certain annotations into new packages. For example, `@DataJpaTest` is available through `org.springframework.boot.data.jpa.test.autoconfigure`.

This introduced migration work, but the current framework baseline was retained.

**Implementation**

Recorded as implemented. The application has been configured for Spring Boot 4.1.1, and imports were adjusted for the updated package structure.

## Decision 3: PostgreSQL

**Status:** Accepted

**Decision**

Use PostgreSQL 17 as the relational database.

**Reason**

PostgreSQL provides relational integrity, constraints, transactions, and analytical SQL capabilities for reporting. Managed hosting options, such as Neon, also support using PostgreSQL in development and deployment.

**Trade-off**

A database server must be running and accessible, unlike an embedded file-based database. This was accepted to provide realistic database behaviour.

**Implementation**

The database connection was verified through HikariCP, and Flyway migrations were applied against `acme_salary_management` using PostgreSQL 17.6, according to the recorded development results.

**Least-privilege database access**

The application connects using a dedicated `acme_app` role rather than the PostgreSQL superuser. Permissions are scoped to the application's database and schema requirements.

## Decision 4: Flyway for Database Migrations

**Status:** Accepted

**Decision**

Use versioned SQL migrations managed by Flyway.

**Reason**

Schema changes become repeatable, reviewable, and traceable across development and deployment environments.

**Rule**

Never modify an applied migration. Introduce schema changes through a new migration version.

**Trade-off**

Flyway requires more explicit migration management than `spring.jpa.hibernate.ddl-auto=update`, but it provides a safer and more controlled schema-evolution process.

**Implementation**

The following migrations are recorded as implemented:

- `V1__create_employees.sql`
- `V2__create_salary_records.sql`
- `V3__alter_salary_records_currency_code.sql`

V3 corrected the currency column type introduced in V2 after Hibernate schema validation identified a mismatch. The correction was made through a new migration rather than by modifying V2.

Hibernate uses `ddl-auto=validate` to detect inconsistencies between entity mappings and the database schema.

## Decision 5: Controller, Service, and Repository Layers

**Status:** Accepted

**Decision**

Separate responsibilities across three application layers:

- **Controller:** Handles HTTP requests and responses.
- **Service:** Implements business rules.
- **Repository:** Handles persistence and database queries.

**Reason**

Clear separation makes the code easier to test, maintain, and reason about.

**Trade-off**

The design introduces more classes and indirection. This is justified because salary history, currency validation, and employee management involve business rules that should not be embedded in HTTP controllers.

**Implementation**

The employee and salary modules follow this pattern.

- `EmployeeController` and `SalaryRecordController` handle HTTP concerns, parameter validation, and sort-field whitelisting.
- `EmployeeService` and `SalaryRecordService` enforce business rules.
- `EmployeeRepository` and `SalaryRecordRepository` handle persistence.

## Decision 6: Validate at Multiple Levels

**Status:** Accepted

**Decision**

Use Jakarta Bean Validation for request validation and database constraints for data integrity.

**Reason**

Request validation provides useful client-facing errors. Database constraints protect data integrity even when concurrent requests attempt conflicting operations.

**Trade-off**

Rules enforced at both application and database levels must remain consistent.

**Implementation**

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

`GlobalExceptionHandler` maps request-validation and method-validation failures to HTTP 400. Salary-specific rules, including positive amounts, effective-date limits, currency validity, and duplicate effective dates, are enforced by the service and mapped to appropriate HTTP status codes.

## Decision 7: Consistent API Error Responses

**Status:** Accepted

**Decision**

Use a shared API error response structure through a global exception handler.

**Reason**

Clients need predictable error formats for validation failures, missing resources, conflicts, and other expected errors.

**Trade-off**

A shared structure may be more formal than necessary for trivial cases, but consistency simplifies client-side error handling.

**Implementation**

`GlobalExceptionHandler` returns `ApiErrorResponse` for handled validation, type-mismatch, illegal-argument, invalid-currency, duplicate, and not-found cases.

The intended HTTP status mapping is:

| HTTP status | Meaning | Examples |
|---|---|---|
| `400 Bad Request` | Invalid request or parameter | Invalid enum, unsupported sort field, oversized page size, invalid currency |
| `404 Not Found` | Requested resource does not exist | Employee ID not found |
| `409 Conflict` | Request conflicts with existing state | Duplicate employee number or email; duplicate salary effective date |

## Decision 8: Server-Side Pagination, Sorting, Search, and Filtering

**Status:** Accepted

**Decision**

Perform pagination, sorting, search, and filtering at the database query level using Spring Data JPA `Specification` and `Pageable`.

**Reason**

The system must support approximately 10,000 employees without loading every record into application memory or sending the entire directory to the browser.

**Trade-off**

Dynamic query construction introduces complexity, but it supports scalability and a more usable employee directory.

**Implementation**

`EmployeeSpecifications` composes specifications for the supported search and filters.

Both employee and salary controllers validate pagination and sorting parameters against the relevant whitelists and limits before passing a `Pageable` to the service.

## Decision 9: Separate Employee Profiles from Salary History

**Status:** Accepted

**Decision**

Store salary records separately from employee profiles and treat salary history as append-only. Each salary record has an effective date and currency.

Previous salary records must not be overwritten.

**Reason**

Salary history needs to remain auditable. Updating previous records would destroy the record of earlier salary arrangements.

**Trade-off**

The salary table grows over time. Pagination and appropriate indexes support querying the history.

**Implementation**

The append-only design is implemented at three levels:

1. **Database:** `UNIQUE (employee_id, effective_date)` prevents duplicate effective dates for an employee. `ON DELETE RESTRICT` prevents deleting an employee while salary records reference that employee.
2. **Entity:** `SalaryRecord` does not expose setters for `amount`, `currencyCode`, `effectiveDate`, or `employee`.
3. **Service:** `SalaryRecordService` has no update or delete methods. Salary changes are recorded as new inserts.

## Decision 10: Handle Currencies Explicitly

**Status:** Accepted

**Decision**

Store an ISO 4217 currency code with each salary record. Reports must group amounts by currency and must not combine different currencies without a documented conversion methodology.

**Reason**

Adding amounts expressed in different currencies produces misleading results.

**Trade-off**

Cross-currency totals are deferred. The system will provide meaningful per-currency values rather than an incorrect combined total.

**Implementation**

The `currency_code` column uses `VARCHAR(3)` and a database constraint requiring three uppercase letters:

```sql
CHECK (currency_code ~ '^[A-Z]{3}$')
```

The service normalises currency codes to uppercase and validates them using `java.util.Currency.getInstance(...)`. Unknown currency codes cause `InvalidCurrencyException`, which maps to HTTP 400.

The current salary module permits salary records for the same employee to use different currencies. It does not combine those amounts.

The planned reporting module must preserve per-currency grouping.

## Decision 11: Keep Credentials Outside Source Control

**Status:** Accepted

**Decision**

Supply secrets through environment variables loaded from a local `.env` file. Keep `.env` Git-ignored and commit `.env.example` with placeholder values.

**Reason**

This reduces the risk of accidentally committing credentials and follows common configuration-management practices.

**Trade-off**

Each developer must configure their own local environment.

**Implementation**

- The local environment file is `backend/.env`.
- `backend/.env` is Git-ignored, with the ignore rule checked using `git check-ignore`.
- `.env.example` is committed at the repository root and contains placeholder values.
- The loading mechanism is documented in Decision 16.

## Decision 12: Automated Tests Against Real PostgreSQL

**Status:** Accepted

**Decision**

Use JUnit 5 and Spring Boot Test to test business rules and API behaviour. Run repository and specification tests against PostgreSQL rather than an embedded H2 database.

Repository tests use:

```java
@AutoConfigureTestDatabase(replace = Replace.NONE)
```

**Reason**

Flyway migrations, `TIMESTAMPTZ`, `BIGSERIAL`, and database constraints must be tested against the target database engine. H2 may behave differently and could hide database-specific issues.

**Trade-off**

Tests require a running PostgreSQL database. CI will need a managed database or an alternative such as Testcontainers. PostgreSQL-backed tests may take longer than embedded-database tests.

This trade-off was accepted because data correctness is particularly important for salaries and employment history.

**Implementation**

The recorded test result is 61 passing tests across eight test classes:

| Test class | Recorded tests |
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

The salary milestone added 28 tests across the salary controller, repository, and service test classes.

> **Verification note:** Confirm this count by running the complete test suite before submission.

**Data isolation**

`EmployeeRepositoryTest`, `EmployeeSpecificationsTest`, and `SalaryRecordRepositoryTest` use `@Transactional` for rollback after each test and generate unique identifiers to reduce collisions with data created through Postman or previous test runs.

**Follow-up**

Introduce Testcontainers so CI can provision a reproducible PostgreSQL environment without relying on a manually configured database.

## Decision 13: Protect Sensitive Data

**Status:** Accepted

**Decision**

Use Spring Security with JWT authentication and authorisation to protect employee and salary endpoints. Use synthetic data for development and demonstrations.

**Reason**

Salary information is confidential. Unauthenticated write access is unacceptable for production.

**Trade-off**

Authentication introduces login and token-management complexity, but these are necessary for a realistic application.

**Implementation**

Pending. The current development configuration uses `DevSecurityConfig`, guarded by `@Profile("dev")`, to disable CSRF and permit requests for local Postman testing.

Production-ready JWT authentication, authorisation, and security verification remain future work.

## Decision 14: Containerise with Docker

**Status:** Accepted

**Decision**

Provide Dockerfiles for the backend and frontend, plus a `docker-compose.yml` that runs PostgreSQL, the backend, and the frontend.

**Reason**

Containerisation supports a reproducible setup and a clearer deployment process. It reduces environment differences between development and assessment environments.

**Trade-off**

Docker adds configuration and image-build maintenance.

**Implementation**

Pending. Containerisation is planned after the core application features.

## Decision 15: Use LIKE-Based Search for Version 1

**Status:** Accepted

**Decision**

Implement free-text employee search using:

```sql
LOWER(column) LIKE '%term%'
```

The search covers `first_name`, `last_name`, `employee_number`, and `email`. Search matches across these fields are combined with OR, while other filters are combined with the search using AND.

PostgreSQL full-text search using `tsvector`, GIN indexes, and dictionary configuration is deferred.

**Reason**

For the target dataset of approximately 10,000 employees, LIKE-based search provides a simpler initial implementation. Full-text search would introduce additional schema and query complexity, including configuration and special handling for employee numbers and email addresses.

**Trade-off**

LIKE-based substring searches may not scale efficiently to millions of records and do not provide stemming or relevance ranking. If measurements show a performance problem, a `pg_trgm` GIN index or a move to `tsvector` can be considered.

**Implementation**

Implemented in `EmployeeSpecifications.matchesSearch(String)`.

Recorded test coverage includes:

- `matchesSearchFindsByFirstNameCaseInsensitive`
- `matchesSearchFindsByEmailSubstring`
- `fromCriteriaFindsBySearchAndFilters`

## Decision 16: Load `.env` Using `springboot4-dotenv`

**Status:** Accepted

**Decision**

Load the following local environment variables from `backend/.env` through the `springboot4-dotenv` library from `me.paulschwarz`, using its `spring-dotenv-bom` dependency management:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `JWT_EXPIRATION_MS`
- `SPRING_PROFILES_ACTIVE`

The `.env` file remains Git-ignored. `.env.example` at the repository root is the committed template.

**Reason**

Spring Boot does not natively load `.env` files. Without a dotenv integration, developers would need to export variables in each shell session or maintain separate IDE run configurations.

A dotenv library provides a consistent local configuration mechanism for IntelliJ, `mvnw spring-boot:run`, and a packaged JAR, subject to the library's working-directory and loading behaviour.

**Trade-off**

A third-party dependency is introduced. The originally suggested artifact, `me.paulschwarz:spring-dotenv`, targets Spring Boot 3 and did not work with the project's Spring Boot 4 setup. The Spring Boot 4-compatible module and its BOM were selected instead.

A local `.env` file must never be packaged or deployed as a substitute for production configuration. Production secrets and profile selection must come from the hosting environment.

**Additional pitfall**

Values containing `#`, `$`, or whitespace may require double quotes according to the dotenv parser's syntax. Incorrect quoting can truncate values and cause confusing configuration errors.

The `.env.example` template documents this behaviour.

**Implementation**

Recorded as implemented and verified. The backend was reported to start with the `dev` profile and establish a HikariCP database connection without manual shell-level `$env:` exports or IntelliJ-specific environment overrides.

## Decision 17: Disable Security Filters in Controller-Focused Tests

**Status:** Accepted

**Decision**

Both `EmployeeControllerTest` and `SalaryRecordControllerTest` use `@WebMvcTest` and:

```java
@AutoConfigureMockMvc(addFilters = false)
```

Security filters are not exercised in these controller-focused test classes.

**Reason**

After `@Validated` was added to the controllers for method-level parameter validation, Spring Security began rejecting some MockMvc requests with HTTP 401 before the controller logic could execute.

These tests are intended to verify controller behaviour, including parameter binding, status codes, and error responses, rather than authentication.

**Trade-off**

These tests do not verify endpoint protection or CSRF behaviour. Those responsibilities must be covered by dedicated security tests.

**Implementation**

Implemented according to the recorded test configuration. Both controller test classes were reported to pass with security filters disabled.

## Decision 18: Use Semantically Correct HTTP Status Codes

**Status:** Accepted

**Decision**

HTTP status codes must reflect the nature of the failure rather than returning one generic error status.

| Status | Use case | Examples |
|---|---|---|
| `400 Bad Request` | Caller-caused input problems | Invalid body, invalid enum, unsupported sort field, non-numeric parameter, oversized page size, invalid currency |
| `404 Not Found` | Requested resource does not exist | Employee lookup by unknown ID |
| `409 Conflict` | Request conflicts with existing state | Duplicate employee number or email; duplicate salary effective date |

**Reason**

Clients need to distinguish invalid input from a conflict with existing data and from a missing resource. A consistent status-code policy improves error handling.

**Trade-off**

Different failure categories require explicit exception types and mappings. The additional classes make the intended behaviour clearer.

**Implementation**

The recorded implementation includes:

- `EmployeeService.create` throwing `DuplicateEmployeeException` for duplicate employee data.
- `SalaryRecordService.record` throwing `DuplicateSalaryException` and `InvalidCurrencyException` for the corresponding salary failures.
- Both services throwing `EmployeeNotFoundException` when the employee does not exist.
- `EmployeeController.parseSort` and `SalaryRecordController.parseSort` throwing `IllegalArgumentException` for unsupported sort fields, mapped to HTTP 400.

The mappings are covered by controller tests and Postman smoke tests according to the recorded results.

## Decision 19: Resolve Current Salary by Effective Date

**Status:** Accepted

**Decision**

The current salary is the record with the highest `effective_date` on or before today.

Records with a future effective date remain in the salary history but are not treated as current.

**Reason**

Salary increases may be scheduled to take effect in the future. The current-salary endpoint should reflect the salary in effect today, rather than simply returning the most recently inserted record.

A future frontend could provide a separate view of upcoming salary changes without changing this backend rule.

**Trade-off**

When a future raise is recorded, the current-salary endpoint continues to return the existing salary until the new effective date arrives. The salary-history view should make scheduled changes visible.

**Implementation**

`SalaryRecordService.current(...)` uses the repository query:

```java
findTopByEmployeeIdAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
    employeeId,
    LocalDate.now()
)
```

The query is supported by `idx_salary_records_employee_effective_desc`.

Recorded verification includes:

- `SalaryRecordServiceTest.currentReturnsLatestEffectiveRecord`
- `SalaryRecordRepositoryTest.currentResolvesLatestEffectiveDateOnOrBeforeToday`
- `SalaryRecordRepositoryTest.currentReturnsEmptyWhenOnlyFutureDatedRecords`
- A Postman scenario creating past, current, and future salary records and checking that the current endpoint returns the record effective today.

## Decision 20: Use VARCHAR(3), Not CHAR(3), for Currency Codes

**Status:** Accepted

**Decision**

The `salary_records.currency_code` column uses `VARCHAR(3)` rather than `CHAR(3)`.

**Reason**

The initial V2 migration declared `currency_code` as `CHAR(3)`. PostgreSQL stores this as the fixed-width `bpchar` type, which pads values with trailing spaces.

Hibernate's `ddl-auto=validate` mode detected a mismatch between the database column and the entity's `String` mapping with `@Column(length = 3)`, which expects `VARCHAR(3)`.

Instead of disabling schema validation or adapting the entity to `bpchar`, the schema was corrected to use `VARCHAR(3)`, a conventional choice for variable-length text codes.

The existing database constraint remains in place:

```sql
CHECK (currency_code ~ '^[A-Z]{3}$')
```

**Trade-off**

A second migration, `V3__alter_salary_records_currency_code.sql`, was needed because V2 had already been applied. Editing V2 in place would only have been appropriate if it had not yet been applied to any database that needed to be preserved.

The corrective migration follows the immutable-migration rule in Decision 4.

**Implementation**

V3 changes the column type with:

```sql
ALTER TABLE salary_records
    ALTER COLUMN currency_code TYPE VARCHAR(3)
    USING currency_code::varchar(3);
```

The recorded verification includes a successful Flyway migration, subsequent Hibernate schema validation, and confirmation in pgAdmin that `information_schema.columns` reports `character varying(3)` for the currency column.

---

## Maintaining This Document

Update this document when a significant technical decision changes or new evidence affects its status.

- Keep **Accepted** separate from **Implemented** and **Verified**.
- Do not mark functionality as verified until the relevant checks have passed.
- Introduce new database migrations instead of modifying migrations that have already been applied.
- Keep credentials and other sensitive configuration out of version control.
- Update the related architecture and project-structure documentation when implementation status changes.