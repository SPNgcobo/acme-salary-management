# AI Work Log

This document records how AI tools were used while building the ACME Salary Management System, which suggestions were accepted or rejected, and how the resulting work was verified.

The goal is to demonstrate **intentional, critical, and transparent AI use**, rather than AI use alone.

## Entry Format

Each entry records the following information where applicable:

- **Date and task:** When the work took place and what was addressed.
- **Prompt summary:** The request given to the AI tool.
- **AI output:** The main suggestions or proposed solution.
- **Decision:** What was accepted, modified, or rejected, and why.
- **Verification:** Tests, checks, or manual validation performed.
- **Commit:** The related Git commit, where applicable.

> **Record-keeping principle:** Test results in this document are historical evidence from the time of each entry. They must not be treated as proof that the current code passes unless the tests have been run again.

---

## 2025-10-16: Requirements Document Drafting

### Prompt summary

Draft a one-page requirements document for an HR salary management web application supporting 10,000 employees across multiple countries. Emphasise the project scope and explain why certain features are out of scope.

### AI output

Suggested a requirements document structure and an initial list of out-of-scope features.

### Decision

Kept the proposed structure but rewrote the out-of-scope explanations in my own words. Added a currency-handling constraint that was not adequately addressed in the initial suggestion.

### Verification

Reviewed the requirements against the assessment brief and checked that each stated requirement mapped to an in-scope item.

### Related commit

`chore: initialize repository with Spring Boot scaffold, docs, and env template`

---

## 2025-10-17: Architecture and Design Decisions

### Prompt summary

Review the architecture and design decision drafts and identify claims that are not yet supported by the implementation.

### AI output

Flagged statements describing tests and implementation status for features that had not yet been implemented. Recommended a status vocabulary distinguishing accepted decisions, implemented work, verified behaviour, and pending work.

### Decision

Accepted the recommendation and revised the documentation so that implementation and verification claims better reflected the repository. Added a containerisation decision covering Docker and Docker Compose.

### Verification

Reviewed implementation and verification claims against the repository and the available test evidence.

### Related commit

Included in the repository initialisation commit.

---

## 2025-10-17: Secrets Handling and Environment Template

### Prompt summary

Find a suitable way to keep local secrets out of Git while making the project straightforward for reviewers to run.

### AI output

Recommended:

- A Git-ignored `.env` file for local credentials.
- A committed `.env.example` file containing placeholders.
- A `.gitignore` covering Maven, Node.js, IDE files, and secrets.

### Decision

Accepted the approach.

### Verification

Checked Git's ignore rules using:

```powershell
git check-ignore .env
git check-ignore .env.example
```

Expected behaviour:

- `.env` is ignored.
- `.env.example` is not ignored.

The checks should be repeated from the appropriate repository paths if the files are stored in different directories.

### Related commit

`chore: initialize repository with Spring Boot scaffold, docs, and env template`

---

## 2025-10-17: Employee Entity, Repository, and Flyway V1 Migration

### Prompt summary

Design the first backend vertical slice: an employee table migration, JPA entity, repository, and tests. Use Flyway and configure Hibernate with `ddl-auto=validate`.

### AI output

Suggested database columns, constraints, indexes, a JPA entity, and a repository. The initial country-code constraint validated only the string length.

### Decision

Kept most of the proposed schema but strengthened the country-code constraint to require exactly two uppercase letters:

```sql
CHECK (country_code ~ '^[A-Z]{2}$')
```

Kept explicit Java getters and setters rather than introducing Lombok.

### Verification

Ran the Maven test suite.

The command recorded in the original notes was:

```powershell
.\mvnw.cmd clean test
```

**Historical result:**

| Metric | Result |
|---|---:|
| Tests run | 6 |
| Failures | 0 |
| Errors | 0 |
| Build | Success |

### Related commit

`feat(backend): add employee entity, repository, Flyway V1 migration, and repository tests`

---

## 2025-10-18: Fixing Spring Boot 4 Test Infrastructure

### Prompt summary

Investigate why `EmployeeRepositoryTest` could not resolve `@DataJpaTest` and then failed when Spring attempted to replace the data source with an embedded database.

### AI output

Identified two issues:

1. Spring Boot 4 changed the package used by `@DataJpaTest` to `org.springframework.boot.data.jpa.test.autoconfigure`.
2. `@DataJpaTest` attempts to replace the configured data source with an embedded database by default. H2 was not on the classpath.

The suggested options included using `@AutoConfigureTestDatabase(replace = Replace.NONE)` or adding H2.

### Decision

Accepted the import correction and rejected adding H2.

The project relies on PostgreSQL-specific behaviour, including Flyway migrations, `TIMESTAMPTZ`, `BIGSERIAL`, and database constraints. Testing against H2 could conceal compatibility problems.

The trade-off is that repository tests require a running PostgreSQL instance. This decision was documented in Decision 12 of `docs/design-decisions.md`.

### Verification

Ran the Maven test suite.

```powershell
.\mvnw.cmd clean test
```

**Historical result:**

| Metric | Result |
|---|---:|
| Tests run | 6 |
| Failures | 0 |
| Errors | 0 |
| Build | Success |

### Related commit

`test(backend): run @DataJpaTest against real Postgres (Replace.NONE); fix Spring Boot 4 import`

---

## 2025-10-19: Employee API, DTOs, Error Handling, and Web Tests

### Prompt summary

Design the employee API layer with request and response DTOs, validation, a service handling duplicate and not-found scenarios, a controller exposing POST and GET endpoints, a shared error response, and unit and web-layer tests.

### AI output

Suggested the following components and implementation approach.

**API components**

- `EmployeeRequest` record with `@NotBlank`, `@Email`, country-code `@Pattern`, and `@NotNull` for employment status.
- `EmployeeResponse` record with a static `from(Employee)` factory method.
- `EmployeeService` with transactional operations, duplicate checks, and input normalisation.
- `EmployeeController` exposing employee creation, retrieval by ID, and paginated listing.
- `ApiErrorResponse` and `GlobalExceptionHandler`.
- `EmployeeServiceTest` using a mocked repository.
- `EmployeeControllerTest` using `@WebMvcTest`, `@MockitoBean`, and MockMvc.

The initial suggested test counts were five service tests and five controller tests.

### Decisions and modifications

Accepted the overall design with the following changes.

#### 1. Removed unused imports

Removed an unused static import from `EmployeeControllerTest` after the IDE flagged it.

#### 2. Addressed Spring Security in web tests

Initially added `@WithMockUser` and CSRF tokens using `.with(csrf())` after observing HTTP 403 responses. This allowed tests to run through the configured security filters at that stage.

The later search and filtering milestone changed the controller-test approach; see the 2025-10-20 entry.

#### 3. Updated the Jackson import

Changed the test import from `com.fasterxml.jackson.databind.ObjectMapper` to `tools.jackson.databind.ObjectMapper` to match the Jackson 3 package used by the project's Spring Boot 4 configuration.

#### 4. Improved pagination serialisation

Added `WebConfig` with `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)` to address the `PageImpl` serialisation warning and provide a more stable paginated response structure.

#### 5. Added development-only security configuration

Added `DevSecurityConfig` under the `dev` profile to permit requests and disable CSRF for local API testing through Postman or a browser.

This configuration is intended for development only and must not be treated as production security.

#### 6. Improved repository test isolation

Configured `EmployeeRepositoryTest` to use `@Transactional` for rollback after each test and UUID-based employee numbers after discovering that manually created Postman data could collide with test data in the shared PostgreSQL database.

### Verification

Ran the Maven test suite.

```powershell
.\mvnw.cmd clean test
```

**Historical result:**

| Test class | Tests |
|---|---:|
| `EmployeeControllerTest` | 5 |
| `EmployeeRepositoryTest` | 5 |
| `EmployeeServiceTest` | 5 |
| `SalaryManagementApplicationTests` | 1 |
| **Total** | **16** |

| Metric | Result |
|---|---:|
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |
| Build | Success |

### Manual API verification

Tested the API through Postman against a running application with the `dev` profile enabled.

| Scenario | Expected result | Historical result |
|---|---|---|
| Retrieve an empty employee list | HTTP 200 with an empty content array and pagination metadata | Matched |
| Create a valid employee (`EMP-0001`) | HTTP 201 with an employee representation and generated fields | Matched |
| Retrieve the created employee by ID | HTTP 200 with the same employee | Matched |
| Submit invalid data, including a blank employee number, invalid email, and lowercase country code | HTTP 400 with field-level validation details | Matched |
| Create a duplicate employee number | HTTP 409 with a duplicate-number error | Matched |
| Retrieve a non-existent employee ID | HTTP 404 with an employee-not-found error | Matched |

Also checked in pgAdmin that the employee created through POST existed in the PostgreSQL `employees` table.

The Postman collection was committed at:

`docs/postman/acme-salary-management.postman_collection.json`

This allows reviewers to inspect and rerun the documented API scenarios.

### Related commit

`feat(backend): add employee API layer with DTOs, service, controller, error handler, and web tests`

---

## 2025-10-20: Employee Search, Filters, Pagination Validation, and Reproducible Configuration

This milestone extended the employee module with search, filtering, sort-field whitelisting, and pagination validation. It also improved the local configuration workflow to provide a more stable foundation for subsequent development.

### Part 1: Search and filters

#### Prompt summary

Design the search and filter layer for the employee listing endpoint. Provide a single free-text search across employee names, employee number, and email. Support country, department, and employment-status filters, combined search and filtering, a sort-field whitelist, and a maximum page size.

#### AI output

Suggested:

- `EmployeeSearchCriteria` as a value object carrying validated search and filter values.
- `EmployeeSpecifications` as a composable specification factory.
- `EmployeeService.list(EmployeeSearchCriteria, Pageable)` with a fallback to `findAll(pageable)` when criteria are empty.
- `EmployeeController.list(...)` with `@Validated`, method-level parameter constraints, and a `SORTABLE_FIELDS` whitelist.
- `EmployeeRepository` extending `JpaSpecificationExecutor`.

#### Decisions and modifications

**LIKE-based search instead of full-text search**

Used `LOWER(column) LIKE '%term%'` for the initial search implementation. Full-text search was deferred to avoid additional schema and query complexity without demonstrated need at the target scale.

This trade-off is recorded in Decision 15 of `docs/design-decisions.md`.

**Sort-field whitelist at the controller**

Unsupported sort fields raise `IllegalArgumentException`, which is mapped to HTTP 400. This prevents invalid sort fields from causing server errors and prevents clients from selecting arbitrary entity fields.

**Maximum page size of 100**

Page sizes outside the permitted range return HTTP 400.

**Empty criteria handling**

`fromCriteria(...)` returns `null` when no criteria are provided. The service checks for this condition and falls back to `findAll(pageable)` rather than passing a null specification to `JpaSpecificationExecutor`.

**Repository extension**

`EmployeeRepository` now extends `JpaSpecificationExecutor` to support database-level specifications alongside existing repository methods.

### Part 2: Exception mapping refinement

During testing, two new controller tests expected HTTP 400 for an unknown sort field and an invalid enum value, but the handler returned HTTP 409 Conflict.

This exposed a semantic problem: caller-caused `IllegalArgumentException` errors and duplicate-employee business errors were being mapped to the same status.

#### Decisions and modifications

- Introduced `DuplicateEmployeeException` for duplicate employee numbers or email addresses, mapped to HTTP 409.
- Mapped caller-caused `IllegalArgumentException` cases to HTTP 400.
- Added a dedicated `MethodArgumentTypeMismatchException` handler for invalid enum values and numeric parameter conversions.
- Added a dedicated `ConstraintViolationException` handler for method-level parameter validation failures.

The resulting status mapping is:

| HTTP status | Use |
|---|---|
| 400 Bad Request | Invalid input, validation failures, type mismatches, unsupported sorting, and oversized pages. |
| 404 Not Found | Requested employee does not exist. |
| 409 Conflict | Duplicate employee number or email. |

### Part 3: Test infrastructure and local configuration

#### Controller-test security behaviour

Two controller tests initially failed with HTTP 401 because Spring Security rejected MockMvc requests before they reached the controller after `@Validated` was introduced.

The controller test configuration was changed to use:

```java
@AutoConfigureMockMvc(addFilters = false)
```

This isolates controller behaviour from the security filter chain. Dedicated security tests remain necessary to verify authentication and authorisation.

The implementation is documented in Decision 17 of `docs/design-decisions.md`.

The tests no longer require `@WithMockUser` or `.with(csrf())` because the filters are disabled for this test slice.

The test configuration retains:

```java
@Import({
    GlobalExceptionHandler.class,
    MethodValidationPostProcessor.class
})
```

The `MethodValidationPostProcessor` enables method-level validation in the test slice.

#### Reproducible local configuration

Repeatedly setting `$env:DB_*` variables in PowerShell and switching the development profile in IntelliJ proved error-prone.

Setting `spring.profiles.active=dev` in `application.properties` was also rejected because it could activate development settings during tests or production runs.

The resulting configuration approach was:

- Store local variables in `backend/.env`.
- Keep `.env` out of Git.
- Use `SPRING_PROFILES_ACTIVE=dev` for local development.
- Keep development-specific overrides in `application-dev.properties`.
- Let the hosting environment set the production profile and supply production secrets.
- Use the `springboot4-dotenv` integration to load local environment variables.

The initially considered `me.paulschwarz:spring-dotenv:4.0.0` was reported not to load variables under the project's Spring Boot 4 setup. The project therefore adopted `springboot4-dotenv` with the `spring-dotenv-bom` BOM.

**Password handling**

Values in `.env` that contain characters interpreted specially by the dotenv parser, such as `#` or `$`, may need appropriate quoting or escaping. Follow the parser's documented syntax and verify the resulting values rather than assuming all parsers handle them identically.

This setup is documented in Decision 16 of `docs/design-decisions.md`.

#### Test profile precedence

The repository and specification test classes use `@ActiveProfiles("test")` for test-specific configuration. The intended test database configuration remains in `application-test.properties`, with Flyway enabled and Hibernate set to `validate`.

Verify the effective active profiles and datasource from the latest test logs if configuration changes are made.

### Verification

Ran the full Maven test suite.

```powershell
.\mvnw.cmd clean test
```

**Historical result:**

| Test class | Tests |
|---|---:|
| `EmployeeControllerTest` | 10 |
| `EmployeeRepositoryTest` | 5 |
| `EmployeeServiceTest` | 8 |
| `EmployeeSpecificationsTest` | 9 |
| `SalaryManagementApplicationTests` | 1 |
| **Total** | **33** |

| Metric | Result |
|---|---:|
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |
| Build | Success |

These results describe the test run recorded at the time of this entry. They do not replace a fresh test run before submission.

### Manual API verification

The application was started using Maven from a fresh PowerShell session without shell-level environment-variable exports or IntelliJ run-configuration overrides.

The recorded startup logs indicated:

- The `dev` profile was active.
- HikariCP established a database connection.
- The database URL pointed to `acme_salary_management`.
- The database reported PostgreSQL 17.6.
- Tomcat started on port 8080.

The following Postman scenarios were recorded as matching their expected results:

| Request | Expected result | Historical result |
|---|---|---|
| `GET /api/employees?search=ada` | HTTP 200 with matching results | Matched |
| `GET /api/employees?country=GB` | HTTP 200 with filtered results | Matched |
| `GET /api/employees?sort=lastName,asc&size=5` | HTTP 200 with sorted results | Matched |
| `GET /api/employees?sort=passwordHash,asc` | HTTP 400 for unsupported sort field | Matched |
| `GET /api/employees?status=BANANA` | HTTP 400 for invalid employment status | Matched |
| `GET /api/employees?size=500` | HTTP 400 for page size above the limit | Matched |
| `GET /api/employees?country=GBR` | HTTP 400 for invalid country format | Matched |

The Postman collection was updated to include these scenarios:

`docs/postman/acme-salary-management.postman_collection.json`

### Related commit

`feat(backend): add employee search, filters, sort whitelist, and reproducible local configuration`

---

## Remaining Verification and Implementation Work

The following items were recorded as outstanding at the time of this log entry:

- Salary module, including the V2 migration, append-only salary history, and currency handling.
- Reporting module, including per-currency and per-department reports.
- JWT authentication and endpoint protection, replacing the development-only security configuration.
- Dedicated security tests for unauthenticated and unauthorised requests.
- Angular frontend.
- Deterministic seed-data generator for 10,000 employees and associated salary records.
- Dockerfiles and `docker-compose.yml`.
- Deployment and demonstration video.

Update this list as work is completed and verified.

## Record-Keeping Rules

1. Never record a test, feature, or security control as verified unless the relevant check has actually been run and its result is available.
2. Keep historical test results distinguishable from current results.
3. Record which AI suggestions were accepted, modified, or rejected, and explain why.
4. Include relevant test commands, manual checks, and commit references where available.
5. Update this document when new AI-assisted implementation work, technical decisions, or verification results occur.
6. Keep the work log consistent with `docs/architecture.md`, `docs/design-decisions.md`, and `docs/project-structure.md`.

The purpose of this work log is to demonstrate that AI-assisted development was reviewed critically, tested where appropriate, and guided by independent technical decisions.