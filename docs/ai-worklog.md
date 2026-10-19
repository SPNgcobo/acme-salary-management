# AI Work Log

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

**Prompt**

> Draft a one-page requirements document for an HR salary management web application for 10,000 employees across multiple countries. Emphasise scope and explain why certain features are out of scope.

**AI Output**

Suggested a requirements document structure and an initial list of out-of-scope features.

**Decision**

Kept the proposed structure but rewrote the out-of-scope explanations in my own words. Added a currency-handling constraint that was not adequately addressed in the initial suggestion.

**Verification**

Reviewed the document against the assessment brief and checked that each stated requirement mapped to an in-scope item.

**Commit**

`chore: initialize repository with Spring Boot scaffold, docs, and env template`

---

## 2025-10-17 — Architecture and Design Decisions

**Prompt**

> Review my architecture and design decision drafts and flag claims that are not yet backed by code.

**AI Output**

Flagged statements describing tests and implementation status for features that had not yet been implemented. Recommended a status vocabulary to distinguish accepted decisions, implemented work, verified behaviour, and pending work.

**Decision**

Accepted the recommendation and revised the documents so that their status claims better reflected the repository. Added a containerisation decision covering Docker and Docker Compose.

**Verification**

Reviewed implementation and verification claims against the actual repository and available test evidence.

**Commit**

Included in the repository initialisation commit.

---

## 2025-10-17 — Secrets Handling and Environment Template

**Prompt**

> What is the best way to keep local secrets out of Git while making the project easy for reviewers to run?

**AI Output**

Recommended:

- A Git-ignored `.env` file for local credentials.
- A committed `.env.example` file containing placeholders.
- A `.gitignore` covering Maven, Node.js, IDE files, and secrets.

**Decision**

Accepted the approach.

**Verification**

Checked Git's ignore rules:

```powershell
git check-ignore .env
git check-ignore .env.example
```

Expected behaviour:

- `.env` is ignored.
- `.env.example` is not ignored.

**Commit**

`chore: initialize repository with Spring Boot scaffold, docs, and env template`

---

## 2025-10-17 — Employee Entity, Repository, and Flyway V1 Migration

**Prompt**

> Design the first vertical slice: employee table migration, JPA entity, repository, and tests. Use Flyway and `ddl-auto=validate`.

**AI Output**

Suggested database columns, constraints, indexes, a JPA entity, and a repository. The initial country-code check validated only the string length.

**Decision**

Kept most of the proposed schema but tightened the country-code constraint to:

```sql
CHECK (country_code ~ '^[A-Z]{2}$')
```

Kept explicit Java getters and setters rather than introducing Lombok.

**Verification**

Ran the Maven test suite:

```powershell
.\mvnw.cmd clean test
```

Recorded result at the time:

```text
Tests run: 6, Failures: 0, Errors: 0
BUILD SUCCESS
```

**Commit**

`feat(backend): add employee entity, repository, Flyway V1 migration, and repository tests`

---

## 2025-10-18 — Fixing Spring Boot 4 Test Infrastructure

**Prompt**

> EmployeeRepositoryTest fails with `Cannot resolve symbol DataJpaTest` and then `Failed to replace DataSource with an embedded database for tests`.

**AI Output**

Identified two issues and proposed corresponding fixes:

1. Spring Boot 4 changed the package used for `@DataJpaTest` to `org.springframework.boot.data.jpa.test.autoconfigure`.
2. `@DataJpaTest` attempts to replace the configured data source with an embedded database by default. Because H2 was not on the classpath, the test configuration needed adjustment.

The proposed options included using `@AutoConfigureTestDatabase(replace = Replace.NONE)` or adding H2.

**Decision**

Accepted the import correction and deliberately rejected adding H2.

The reason was that the project relies on PostgreSQL-specific behaviour, including Flyway migrations, `TIMESTAMPTZ`, `BIGSERIAL`, and check constraints. Testing against H2 could conceal database compatibility issues.

The trade-off is that repository tests require a running PostgreSQL instance. The decision was documented as Decision 12 in `design-decisions.md`.

**Verification**

Ran the combined test suite:

```powershell
.\mvnw.cmd clean test
```

Recorded result at the time:

```text
Tests run: 6, Failures: 0, Errors: 0
BUILD SUCCESS
```

**Commit**

`test(backend): run @DataJpaTest against real Postgres (Replace.NONE); fix Spring Boot 4 import`

---

## 2025-10-19 — Employee API, DTOs, Error Handling, and Web Tests

**Prompt**

> Design the employee API layer: request and response DTOs with validation, a service that handles business rules such as duplicate detection and not-found scenarios, a controller exposing POST and GET endpoints, and a consistent error response through a global exception handler. Add unit tests for the service and web-layer tests with MockMvc.

**AI Output**

Suggested the following components and implementation approach.

### API Components

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

Accepted the overall design, with the following implementation changes:

1. **Removed an unused import.** Removed `import static org.mockito.ArgumentMatchers.eq;` from `EmployeeControllerTest` after the IDE flagged it as unused.

2. **Handled Spring Security in web tests.** Added `@WithMockUser` and CSRF tokens using `.with(csrf())` to POST tests after observing HTTP 403 responses. This allowed the tests to exercise the controller under the configured security filters rather than bypassing those filters.

3. **Updated the Jackson import.** Changed the test import from `com.fasterxml.jackson.databind.ObjectMapper` to `tools.jackson.databind.ObjectMapper` to match the Jackson 3 package used by the project's Spring Boot 4 configuration.

4. **Improved pagination serialisation.** Added `WebConfig` with `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)` to address the `PageImpl` serialisation warning and provide a more stable paginated response structure.

5. **Added development-only security configuration.** Added `DevSecurityConfig` under the `dev` profile to permit requests and disable CSRF for local API testing through Postman or a browser. This configuration is intended for development only and must not be used as production security.

6. **Improved repository test isolation.** Made `EmployeeRepositoryTest` use `@Transactional` for rollback after each test and UUID-based employee numbers after discovering that manually created Postman data could collide with test data in the shared PostgreSQL database.

### Verification

Ran the Maven test suite:

```powershell
.\mvnw.cmd clean test
```

Recorded result at the time:

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

The API was tested through Postman against a running application with the `dev` profile enabled.

| # | Scenario | Expected result | Recorded result |
|---:|---|---|---|
| 1 | GET an empty employee list | HTTP 200 with an empty content array and pagination metadata | Matched |
| 2 | POST a valid employee (`EMP-0001`) | HTTP 201 with an employee representation and generated fields | Matched |
| 3 | GET the created employee by ID | HTTP 200 with the same employee | Matched |
| 4 | POST invalid data, including a blank employee number, invalid email, and lowercase country code | HTTP 400 with field-level validation details | Matched |
| 5 | POST a duplicate employee number | HTTP 409 with a duplicate-number error | Matched |
| 6 | GET a non-existent employee ID | HTTP 404 with an employee-not-found error | Matched |

Also checked in pgAdmin that the employee created through POST existed in the PostgreSQL `employees` table.

The Postman collection was committed at:

`docs/postman/acme-salary-management.postman_collection.json`

This allows reviewers to inspect and rerun the documented API scenarios.

**Commit**

`feat(backend): add employee API layer with DTOs, service, controller, error handler, and web tests`

---

## Remaining Verification Work

The following items were still outstanding when this entry was recorded:

- Employee search and filter specifications, including query-parameter validation.
- Salary module, including the V2 migration, append-only history, and currency handling.
- Reporting module, including per-currency and per-department reporting.
- JWT authentication and endpoint protection, replacing `DevSecurityConfig`.
- Angular frontend.
- Deterministic seed-data generator for 10,000 employees.
- Dockerfiles and `docker-compose.yml`.
- Deployment and demonstration video.

Update this list as work is completed and verified.

## Record-Keeping Rule

Never record a test, feature, or security control as verified unless the relevant check has actually been run and its result is available.

Historical results must remain distinguishable from current results. Update this work log when new implementation decisions, AI-assisted changes, verification results, or related commits are made.
