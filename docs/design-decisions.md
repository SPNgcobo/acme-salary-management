# ACME Salary Management System: Design Decisions

This document records the key technical decisions for the ACME Salary Management System, including the reasoning behind each decision, its trade-offs, and its implementation status.

A decision may be **Accepted** while its implementation remains **Pending**.

## Status Legend

| Status | Meaning |
|---|---|
| **Accepted** | The technical decision has been agreed upon. |
| **Implemented** | The corresponding code or configuration exists. |
| **Verified** | The implementation has been checked through passing tests or observed behaviour. |
| **Pending** | The implementation has not yet been completed. |

---

## Decision 1: Modular Monolith

**Status:** Accepted

**Decision**

Build one Spring Boot application divided into employee, salary, reporting, security, and common modules.

**Reason**

The project scope does not justify the operational complexity of microservices. A single deployable application is simpler to develop, test, run, and demonstrate.

**Trade-offs**

Module boundaries must be maintained intentionally to prevent the codebase from becoming tightly coupled and difficult to maintain.

**Implementation status**

The modular structure is implemented for the `common` and `employee` packages and supporting components. The salary, reporting, and production security implementations remain pending.

---

## Decision 2: Java 21 and Spring Boot 4.1.1

**Status:** Accepted

**Decision**

Use Java 21 LTS and Spring Boot 4.1.1 for the backend.

**Reason**

Java provides a stable LTS toolchain, while Spring Boot supports REST APIs, dependency injection, validation, persistence, security, and automated testing.

**Trade-offs**

Spring Boot 4 introduces changes to its modularised test dependencies and package locations. For example, `@DataJpaTest` is available under:

```java
org.springframework.boot.data.jpa.test.autoconfigure
```

These changes require additional configuration and migration effort but are manageable.

**Implementation status**

Recorded as implemented. The application has previously been reported to build and run with Spring Boot 4.1.1, with the relevant imports corrected for the updated package structure.

**Verification:** Confirm the version in `backend/pom.xml` and run the current build before submission.

---

## Decision 3: PostgreSQL

**Status:** Accepted

**Decision**

Use PostgreSQL 17 as the relational database.

**Reason**

PostgreSQL provides relational integrity, constraints, transactions, and SQL capabilities suitable for employee management and salary reporting. Managed hosting options, such as Neon, can also support deployment.

**Trade-offs**

Unlike an embedded database, PostgreSQL requires a running database server. This additional setup is accepted because testing against the intended database engine provides more realistic behaviour.

**Implementation status**

Recorded as implemented. The database connection was previously verified through HikariCP, and the initial Flyway migration was applied to `acme_salary_management` on PostgreSQL 17.6.

**Least-privilege database access**

The application connects using the dedicated `acme_app` database role rather than the `postgres` superuser. Grants are intended to be restricted to the application's database and schema requirements.

**Verification:** Confirm the current database version, grants, connection configuration, and migration status before submission.

---

## Decision 4: Flyway for Database Migrations

**Status:** Accepted

**Decision**

Manage schema changes through versioned SQL migrations using Flyway.

**Reason**

Versioned migrations make database changes repeatable, reviewable, and traceable across development, testing, and deployment environments.

**Rules**

- Never modify an already-applied migration in a shared or persistent environment.
- Introduce schema changes through a new migration version.
- Keep entity mappings consistent with the database schema.

**Trade-offs**

Flyway requires more explicit migration management than automatically updating the schema through Hibernate. This additional discipline reduces the risk of unexpected schema changes.

**Implementation status**

Implemented. The initial migration is:

`backend/src/main/resources/db/migration/V1__create_employees.sql`

Hibernate uses `ddl-auto=validate` to validate entity mappings against the managed schema rather than create or modify tables automatically.

---

## Decision 5: Controller, Service, and Repository Layers

**Status:** Accepted

**Decision**

Separate backend responsibilities into three primary layers:

- **Controller:** HTTP requests, request parameters, and response status codes.
- **Service:** Business rules, operation coordination, and entity-to-DTO mapping.
- **Repository:** Database access and persistence.

**Reason**

Separating responsibilities makes the code easier to understand, test, and maintain.

**Trade-offs**

The layered approach introduces additional classes and indirection. This is accepted because employee management and salary history contain business rules that should not be mixed with HTTP or database concerns.

**Implementation status**

Implemented for the employee module.

- `EmployeeController` handles HTTP concerns, parameter validation, and sort-field whitelisting.
- `EmployeeService` handles business logic, duplicate detection, search-criteria processing, and entity-to-DTO mapping.
- `EmployeeRepository` extends `JpaRepository` and `JpaSpecificationExecutor`.

---

## Decision 6: Validate at Multiple Levels

**Status:** Accepted

**Decision**

Use Jakarta Bean Validation for incoming requests and database constraints to protect data integrity.

**Reason**

Request validation produces useful client-facing errors. Database constraints protect integrity even when multiple requests are processed concurrently or data is written through another application path.

**Trade-offs**

Some rules exist at both the application and database levels. These rules must remain consistent.

**Implementation status**

Implemented for employee management.

`EmployeeRequest` uses validation annotations such as:

- `@NotBlank`
- `@Email`
- `@Pattern`
- `@NotNull`

The employee listing endpoint uses method-level validation, including `@Min`, `@Max`, `@Size`, and `@Pattern`, with `@Validated` enabled on the controller.

Database constraints, including `UNIQUE` and `CHECK` constraints where appropriate, provide additional protection.

`GlobalExceptionHandler` maps body-validation failures represented by `MethodArgumentNotValidException` and method-level parameter-validation failures represented by `ConstraintViolationException` to HTTP 400 responses.

---

## Decision 7: Consistent API Error Responses

**Status:** Accepted

**Decision**

Use a shared API error response structure through `ApiErrorResponse` and `GlobalExceptionHandler`.

**Reason**

Consistent error responses make it easier for clients to handle validation failures, missing resources, duplicate records, and other expected errors.

**Trade-offs**

A shared response structure introduces a little more implementation effort for simple cases. The consistency is worth the cost.

**Implementation status**

Implemented.

`GlobalExceptionHandler` handles supported validation, type-mismatch, illegal-argument, duplicate-record, and not-found exceptions.

The intended status codes are:

| HTTP status | Meaning | Example |
|---|---|---|
| `400 Bad Request` | Invalid or unsupported request input. | Invalid status, unsupported sort field, or oversized page size. |
| `404 Not Found` | The requested resource does not exist. | Employee ID not found. |
| `409 Conflict` | The request conflicts with existing application state. | Duplicate employee number or email. |

These mappings are covered by the employee controller tests.

---

## Decision 8: Server-Side Pagination, Sorting, Search, and Filtering

**Status:** Accepted

**Decision**

Perform employee search, filtering, sorting, and pagination through Spring Data JPA `Specification` and `Pageable`.

**Reason**

The application is intended to support approximately 10,000 employees. Loading every employee into memory or returning all records to the browser would be inefficient.

**Trade-offs**

Database-level filtering requires more complex query construction and testing. This is accepted because it improves scalability and reduces unnecessary data transfer.

**Implementation status**

Implemented.

`EmployeeSpecifications` builds reusable predicates for search and filtering.

- Search matches first name, last name, employee number, or email, using OR between searchable fields.
- Country and employment-status filters use exact matching.
- Department matching is case-insensitive.
- Search criteria and separate filters are combined using AND.
- `EmployeeController.list(...)` validates page numbers, page sizes, and sort parameters.
- Page numbers must be non-negative.
- Page sizes must be between 1 and 100.
- Sort fields must appear on an explicit whitelist.

The service passes the resulting specification and pageable to `JpaSpecificationExecutor.findAll(...)`.

---

## Decision 9: Separate Employee Profiles from Salary History

**Status:** Accepted

**Decision**

Store employee profiles separately from salary records. Salary records should contain an effective date and currency code and should be append-only at the application level.

**Reason**

Salary history must be auditable. Overwriting an earlier salary would lose information about previous compensation.

**Trade-offs**

The salary table will grow over time. Appropriate pagination, indexing, and query design will help manage that growth.

**Implementation status:** Pending.

The `salary_records` table is planned for the V2 migration. Previous salary records should remain available when a new salary takes effect.

---

## Decision 10: Handle Currencies Explicitly

**Status:** Accepted

**Decision**

Store an ISO 4217 currency code with every salary record. Group reports by currency and do not calculate combined cross-currency totals without a documented conversion method.

**Reason**

Adding amounts in different currencies produces misleading financial figures.

**Trade-offs**

Cross-currency totals are deferred. Users will initially receive meaningful totals for each currency rather than an incorrect combined total.

**Implementation status:** Pending.

The currency-aware salary and reporting features will be implemented with the salary and reporting modules.

---

## Decision 11: Keep Credentials Outside Source Control

**Status:** Accepted

**Decision**

Supply secrets through environment variables. For local development, load them from a git-ignored `.env` file. Commit `.env.example` as a template containing placeholder values only.

**Reason**

Keeping credentials out of source control reduces the risk of accidental disclosure.

**Trade-offs**

Each development environment requires its own local configuration.

**Implementation status**

Recorded as implemented.

- Local configuration file: `backend/.env`
- Template: `.env.example` at the repository root.
- `.env` is intended to be excluded from Git.

The ignore rule can be checked from the repository root with:

```powershell
git check-ignore .\backend\.env
```

The command should identify the file as ignored.

See Decision 16 for how local environment variables are loaded.

---

## Decision 12: Automated Tests Against PostgreSQL

**Status:** Accepted

**Decision**

Use JUnit 5 and Spring Boot Test, with tests covering business rules, API behaviour, and database persistence.

Repository and specification tests run against PostgreSQL rather than an embedded H2 database. The `@DataJpaTest` configuration uses:

```java
@AutoConfigureTestDatabase(replace = Replace.NONE)
```

**Reason**

The schema uses PostgreSQL-specific behaviour, including `TIMESTAMPTZ`, `BIGSERIAL`, and database constraints. Testing against PostgreSQL helps identify migration and database-dialect issues that an embedded database could hide.

**Trade-offs**

Tests require a running PostgreSQL instance, unless a managed test database or Testcontainers is used. This introduces setup requirements but improves confidence in database correctness.

**Implementation status**

The following test counts were recorded previously:

| Test class | Recorded tests |
|---|---:|
| `EmployeeControllerTest` | 10 |
| `EmployeeRepositoryTest` | 5 |
| `EmployeeServiceTest` | 8 |
| `EmployeeSpecificationsTest` | 9 |
| `SalaryManagementApplicationTests` | 1 |
| **Total** | **33** |

These results must be confirmed by the latest test run before submission.

**Data isolation**

`EmployeeRepositoryTest` and `EmployeeSpecificationsTest` use `@Transactional` to roll back test changes. Unique identifiers are generated for test methods to reduce collisions with data created manually or by earlier test runs.

**Follow-up**

Consider adding Testcontainers so CI can start a disposable PostgreSQL instance without relying on a manually provisioned database.

---

## Decision 13: Protect Sensitive Data

**Status:** Accepted

**Decision**

Use Spring Security with JWT authentication to protect employee and salary endpoints. Use synthetic employee data during development and demonstrations.

**Reason**

Salary information is confidential. Unauthenticated access to sensitive operations is unacceptable for a production application.

**Trade-offs**

Authentication introduces login, token handling, and additional testing requirements. These are accepted because they reflect realistic application security requirements.

**Implementation status:** In Progress.

Spring Security is included, but production-grade JWT authentication and authorisation remain pending.

A development-only `DevSecurityConfig`, guarded by `@Profile("dev")`, disables CSRF and permits requests when the development profile is active. This is intended for local Postman testing only.

The effective security behaviour outside the `dev` profile depends on the remaining configuration and Spring Security defaults. The production security milestone must replace this development setup with verified authentication and access-control rules.

---

## Decision 14: Containerise with Docker

**Status:** Accepted

**Decision**

Provide Dockerfiles for the backend and frontend, plus a `docker-compose.yml` configuration for PostgreSQL, the backend, and the frontend.

**Reason**

Containerisation improves reproducibility, simplifies setup for reviewers, and reduces environment-specific differences between development and deployment.

**Trade-offs**

Docker introduces additional build configuration and container images that must be maintained.

**Implementation status:** Pending.

The planned files are:

| File | Purpose |
|---|---|
| `backend/Dockerfile` | Build and run the Spring Boot backend. |
| `frontend/Dockerfile` | Build the Angular frontend and serve it through Nginx. |
| `docker-compose.yml` | Coordinate the database, backend, and frontend services. |

Containerisation will follow the core application features.

---

## Decision 15: Use LIKE-Based Search for Version 1

**Status:** Accepted

**Decision**

Use case-insensitive partial matching with `LOWER(column) LIKE '%term%'` across:

- `first_name`
- `last_name`
- `employee_number`
- `email`

The searchable fields are combined with OR, while additional filters are combined with the search criteria using AND.

PostgreSQL full-text search using `tsvector` and a GIN index is deferred.

**Reason**

The initial target is approximately 10,000 employees. A straightforward LIKE-based approach is easier to implement and understand than a full-text search configuration with additional indexing and query rules.

Full-text search also requires consideration of prefix matching for employee numbers and email addresses, which may not align with its default tokenisation behaviour.

**Trade-offs**

LIKE-based search may become less efficient at larger data volumes and does not provide stemming or relevance ranking.

If performance measurements justify it, a PostgreSQL trigram index using `pg_trgm` or a move to `tsvector` can be considered.

**Implementation status**

Implemented in `EmployeeSpecifications.matchesSearch(String)`.

Previously recorded tests include:

- `matchesSearchFindsByFirstNameCaseInsensitive`
- `matchesSearchFindsByEmailSubstring`
- `fromCriteriaFindsBySearchAndFilters`

Confirm that these tests still pass before marking the decision as verified.

---

## Decision 16: Load Local Environment Variables Using `springboot4-dotenv`

**Status:** Accepted

**Decision**

Use the `springboot4-dotenv` integration from `me.paulschwarz` to load local environment variables from `backend/.env`.

The intended configuration includes:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `JWT_EXPIRATION_MS`
- `SPRING_PROFILES_ACTIVE`

The dependency is managed through the `spring-dotenv-bom` BOM, as configured in the project.

The `.env` file is git-ignored, while `.env.example` at the repository root documents the required variables using placeholders.

**Reason**

Spring Boot does not natively load `.env` files. Without an integration, developers must export variables separately in each shell session or configure them manually in their IDE.

A dotenv integration is intended to simplify local configuration across IntelliJ, Maven commands, and packaged application runs.

**Trade-offs**

The integration introduces a third-party dependency that must remain compatible with the chosen Spring Boot version.

The project selected `springboot4-dotenv` rather than the originally considered `me.paulschwarz:spring-dotenv`, because the latter was reported not to load the required variables under Spring Boot 4.

The `.env` file must remain a local development configuration file. Production secrets should be provided by the hosting environment or an appropriate secrets manager.

**Implementation status**

Recorded as implemented and previously verified.

Earlier checks reported that the application started with the `dev` profile active and established a HikariCP database connection without requiring shell-level environment-variable exports or IDE-specific overrides.

Re-run these checks after dependency or configuration changes.

---

## Decision 17: Disable Security Filters in Controller-Slice Tests

**Status:** Accepted

**Decision**

Configure `EmployeeControllerTest` with:

```java
@WebMvcTest(controllers = EmployeeController.class)
@AutoConfigureMockMvc(addFilters = false)
```

Security filters are not exercised by this controller-focused test class.

**Reason**

The controller tests focus on HTTP behaviour, request validation, parameter binding, and error responses.

The project previously encountered HTTP 401 responses before requests reached the controller after method-level validation was enabled. Disabling filters isolates controller behaviour from security configuration.

**Trade-offs**

These tests do not verify authentication, authorisation, or CSRF behaviour.

Those concerns must be covered by separate security tests in the security milestone.

**Implementation status**

Implemented. The controller test class was previously reported to pass with this configuration.

---

## Decision 18: Use Semantically Correct HTTP Status Codes

**Status:** Accepted

**Decision**

Return HTTP status codes that reflect the type of failure instead of using one generic error status.

| HTTP status | Use case |
|---|---|
| **400 Bad Request** | Invalid request input, body validation failures, method-level validation failures, unknown sort fields, invalid enum values, numeric parameter type mismatches, and oversized page sizes. |
| **404 Not Found** | The requested employee does not exist. |
| **409 Conflict** | A valid request conflicts with existing application state, such as a duplicate employee number or email. |

**Reason**

Clients need to distinguish invalid input from a missing resource or a conflict with existing data. These categories require different handling.

**Trade-offs**

The implementation requires explicit exception types and mappings. This additional structure makes the error-handling behaviour more predictable.

**Implementation status**

Implemented.

- `EmployeeService.create` throws `DuplicateEmployeeException` for duplicate employee numbers or email addresses.
- `EmployeeService.getById` throws `EmployeeNotFoundException` when an employee cannot be found.
- `EmployeeController.parseSort` throws `IllegalArgumentException` for unsupported sort fields or directions.
- `GlobalExceptionHandler` maps supported input errors to HTTP 400, duplicate records to HTTP 409, and missing employees to HTTP 404.

Previously recorded controller tests include:

- `listReturns400WhenSortFieldIsNotWhitelisted`
- `listReturns400WhenStatusIsInvalid`
- `listReturns400WhenSizeExceedsCap`
- `listReturns400WhenCountryFormatIsInvalid`
- `getByIdReturns404WhenMissing`
- `createReturns400WhenValidationFails`

Confirm the current test results before submission.

---

## Review and Maintenance Rules

To keep this document accurate as development continues:

1. Update a decision when its reasoning, trade-offs, or implementation approach changes.
2. Keep **Accepted** separate from **Implemented** and **Verified**.
3. Do not describe planned functionality as completed.
4. Run relevant tests before marking implementation as verified.
5. Update this document in the same commit as significant architectural changes.
6. Keep the architecture and implementation descriptions consistent with `docs/architecture.md` and `docs/project-structure.md`.
7. Never commit real credentials, secrets, or personal employee data.

This document is the record of the technical reasoning behind the ACME Salary Management System and should evolve alongside the codebase.