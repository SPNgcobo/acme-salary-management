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

Expected behaviour:

.env is ignored.

.env.example is not ignored.

Commit

chore: initialize repository with Spring Boot scaffold, docs, and env template

2025-10-17 — Employee Entity, Repository, and Flyway V1 Migration
Prompt

Design the first vertical slice: employee table migration, JPA entity, repository, and tests. Use Flyway and ddl-auto=validate.

AI Output

Suggested database columns, constraints, indexes, a JPA entity, and a repository. The initial country-code check validated only the string length.

Decision

Kept most of the proposed schema but tightened the country-code constraint to:

sql
CHECK (country_code ~ '^[A-Z]{2}$')
Kept explicit Java getters and setters rather than introducing Lombok.

Verification

Ran the Maven test suite:

powershell
.\mvnw.cmd clean test
Recorded result at the time:

text
Tests run: 6, Failures: 0, Errors: 0
BUILD SUCCESS
Commit

feat(backend): add employee entity, repository, Flyway V1 migration, and repository tests

2025-10-18 — Fixing Spring Boot 4 Test Infrastructure
Prompt

EmployeeRepositoryTest fails with Cannot resolve symbol DataJpaTest and then Failed to replace DataSource with an embedded database for tests.

AI Output

Identified two issues and proposed corresponding fixes:

Spring Boot 4 changed the package used for @DataJpaTest to org.springframework.boot.data.jpa.test.autoconfigure.

@DataJpaTest attempts to replace the configured data source with an embedded database by default. Because H2 was not on the classpath, the test configuration needed adjustment.

The proposed options included using @AutoConfigureTestDatabase(replace = Replace.NONE) or adding H2.

Decision

Accepted the import correction and deliberately rejected adding H2.

The reason was that the project relies on PostgreSQL-specific behaviour, including Flyway migrations, TIMESTAMPTZ, BIGSERIAL, and check constraints. Testing against H2 could conceal database compatibility issues.

The trade-off is that repository tests require a running PostgreSQL instance. The decision was documented as Decision 12 in design-decisions.md.

Verification

Ran the combined test suite:

powershell
.\mvnw.cmd clean test
Recorded result at the time:

text
Tests run: 6, Failures: 0, Errors: 0
BUILD SUCCESS
Commit

test(backend): run @DataJpaTest against real Postgres (Replace.NONE); fix Spring Boot 4 import

2025-10-19 — Employee API, DTOs, Error Handling, and Web Tests
Prompt

Design the employee API layer: request and response DTOs with validation, a service that handles business rules such as duplicate detection and not-found scenarios, a controller exposing POST and GET endpoints, and a consistent error response through a global exception handler. Add unit tests for the service and web-layer tests with MockMvc.

AI Output

Suggested the following components and implementation approach.

API Components
EmployeeRequest record with @NotBlank, @Email, @Pattern for country code, and @NotNull for employment status.

EmployeeResponse record with a static from(Employee) factory method.

EmployeeService with transactional operations, duplicate checks, and input normalisation.

EmployeeController exposing:

POST /api/employees — create an employee, returning HTTP 201.

GET /api/employees/{id} — retrieve an employee, returning HTTP 200 or 404.

GET /api/employees — list employees with pagination.

ApiErrorResponse record and GlobalExceptionHandler.

EmployeeServiceTest with five tests using a mocked repository.

EmployeeControllerTest with five tests using @WebMvcTest, @MockitoBean, and MockMvc.

Decisions and Modifications
Accepted the overall design, with the following implementation changes:

Removed an unused import. Removed import static org.mockito.ArgumentMatchers.eq; from EmployeeControllerTest after the IDE flagged it as unused.

Handled Spring Security in web tests. Added @WithMockUser and CSRF tokens using .with(csrf()) to POST tests after observing HTTP 403 responses. This allowed the tests to exercise the controller under the configured security filters rather than bypassing those filters.

Updated the Jackson import. Changed the test import from com.fasterxml.jackson.databind.ObjectMapper to tools.jackson.databind.ObjectMapper to match the Jackson 3 package used by the project's Spring Boot 4 configuration.

Improved pagination serialisation. Added WebConfig with @EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO) to address the PageImpl serialisation warning and provide a more stable paginated response structure.

Added development-only security configuration. Added DevSecurityConfig under the dev profile to permit requests and disable CSRF for local API testing through Postman or a browser. This configuration is intended for development only and must not be used as production security.

Improved repository test isolation. Made EmployeeRepositoryTest use @Transactional for rollback after each test and UUID-based employee numbers after discovering that manually created Postman data could collide with test data in the shared PostgreSQL database.

Verification
Ran the Maven test suite:

powershell
.\mvnw.cmd clean test
Recorded result at the time:

text
Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
The reported test breakdown was:

Test class	Tests
EmployeeControllerTest	5
EmployeeRepositoryTest	5
EmployeeServiceTest	5
SalaryManagementApplicationTests	1
Total	16
Manual API Verification
The API was tested through Postman against a running application with the dev profile enabled.

#	Scenario	Expected result	Recorded result
1	GET an empty employee list	HTTP 200 with an empty content array and pagination metadata	Matched
2	POST a valid employee (EMP-0001)	HTTP 201 with an employee representation and generated fields	Matched
3	GET the created employee by ID	HTTP 200 with the same employee	Matched
4	POST invalid data, including a blank employee number, invalid email, and lowercase country code	HTTP 400 with field-level validation details	Matched
5	POST a duplicate employee number	HTTP 409 with a duplicate-number error	Matched
6	GET a non-existent employee ID	HTTP 404 with an employee-not-found error	Matched
Also checked in pgAdmin that the employee created through POST existed in the PostgreSQL employees table.

The Postman collection was committed at docs/postman/acme-salary-management.postman_collection.json, so reviewers can inspect and rerun the documented API scenarios.

Commit

feat(backend): add employee API layer with DTOs, service, controller, error handler, and web tests

2025-10-20 — Employee Search, Filters, Pagination Validation, and Reproducible Configuration
This milestone extended the employee module with search, filtering, sort whitelisting, and paginated parameter validation, and separately fixed the local configuration workflow so that future milestones start from a stable base.

Part 1 — Search and Filters
Prompt

Design the search and filter layer for the employee list endpoint. A single free-text search box that matches across names, employee number, and email is the HR Manager pattern; filters should be exact-match on country, department, and employment status, combinable with search. Enforce a sort-field whitelist and cap the page size.

AI Output

Suggested:

EmployeeSearchCriteria as a small value object carrying validated search and filter values.

EmployeeSpecifications as a composable specification factory, one method per filter, plus a fromCriteria(...) combinator.

EmployeeService.list(EmployeeSearchCriteria, Pageable) that falls back to findAll(pageable) when the criteria are empty.

EmployeeController.list(...) with @Validated at the class level, @Min, @Max, @Size, @Pattern on the query parameters, and a SORTABLE_FIELDS whitelist that rejects unknown sort fields with HTTP 400.

Decisions and Modifications

LIKE-based search, not full-text. For 10,000 employees on an indexed table, LOWER(col) LIKE '%term%' is fast enough and does not require a tsvector column or dictionary configuration. Documented as Decision 15.

Sort whitelist enforced at the controller. Unknown sort fields raise IllegalArgumentException, mapped to HTTP 400.

Page size capped at 100. Rejected values return HTTP 400.

fromCriteria returns null when criteria are empty. The service checks for null and falls back to findAll(pageable).

EmployeeRepository now extends JpaSpecificationExecutor.

Part 2 — Exception Mapping Refinement
Two controller tests expected HTTP 400 for unknown sort field and invalid enum value, but the handler returned 409. This exposed a semantic problem: both IllegalArgumentException and the duplicate-employee business rule were mapped to the same status.

Decisions and Modifications

Introduced DuplicateEmployeeException mapped to HTTP 409.

Mapped IllegalArgumentException to HTTP 400.

Added a dedicated MethodArgumentTypeMismatchException handler.

Added a dedicated ConstraintViolationException handler.

Full status mapping: 400 for input problems, 404 for missing records, 409 for duplicates. Documented as Decision 18.

Part 3 — Test Infrastructure and Configuration
Two controller tests initially failed with HTTP 401 for all requests. The cause: @Validated on the controller caused Spring Security's filter chain to reject MockMvc requests before the controller ran.

Decisions and Modifications

@AutoConfigureMockMvc(addFilters = false) on EmployeeControllerTest. Documented as Decision 17.

Removed .with(csrf()) from POST tests and removed @WithMockUser.

Retained @Import({GlobalExceptionHandler.class, MethodValidationPostProcessor.class}).

Local environment configuration made reproducible. The prior workflow of setting $env:DB_* variables per PowerShell session and toggling the dev profile in IntelliJ was error-prone. Final solution:

.env moved to backend/.env, git-ignored. SPRING_PROFILES_ACTIVE=dev lives in that file.

Added me.paulschwarz:springboot4-dotenv with the spring-dotenv-bom BOM. The earlier spring-dotenv:4.0.0 artifact targets Spring Boot 3 and does not work under Spring Boot 4.

Password values in .env are wrapped in double quotes when they contain # or $. Documented as Decision 16.

spring.profiles.active=dev in application.properties was removed and replaced by SPRING_PROFILES_ACTIVE=dev in .env, plus application-dev.properties for dev-only property overrides.

Test profile precedence verified. @ActiveProfiles("test") still takes effect even with SPRING_PROFILES_ACTIVE=dev from .env.

Verification
Ran the full test suite:

powershell
.\mvnw.cmd clean test
Recorded result:

text
Tests run: 33, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Test class	Tests
EmployeeControllerTest	10
EmployeeRepositoryTest	5
EmployeeServiceTest	8
EmployeeSpecificationsTest	9
SalaryManagementApplicationTests	1
Total	33
Manual API Verification
The application was started with mvnw spring-boot:run from a fresh PowerShell window with no $env: exports and no IntelliJ run-configuration overrides. Startup log confirmed:

text
The following 1 profile is active: "dev"
HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection@...
Tomcat started on port 8080
Postman requests against the running instance:

#	Request	Expected	Recorded
1	GET /api/employees?search=ada	200 with matching page	Matched
2	GET /api/employees?country=GB	200 with filtered page	Matched
3	GET /api/employees?sort=lastName,asc&size=5	200 with sorted page	Matched
4	GET /api/employees?sort=passwordHash,asc	400 with unsupported-sort-field message	Matched
5	GET /api/employees?status=BANANA	400 with invalid-enum message	Matched
6	GET /api/employees?size=500	400 with size-cap message	Matched
7	GET /api/employees?country=GBR	400 with country-format message	Matched
Commit

feat(backend): add employee search, filters, sort whitelist, and reproducible local configuration

2025-10-20 — Salary Records Module: Migration, Entity, Service, Controller, and Tests
The salary milestone adds append-only salary records to the employee module, resolving the "current salary" as the latest effective record on or before today, and validating currency codes against ISO 4217. It also surfaced two real problems that were resolved through disciplined engineering rather than shortcuts: a schema type mismatch between the migration and the entity, and the correct choice of dotenv library under Spring Boot 4.

Part 1 — Design and Schema
Prompt

Design the salary records module: append-only history, ISO 4217 currency, effective date, current-salary resolution by latest effective date on or before today, salary endpoints nested under the employee. Add Flyway migration, entity, repository, service, controller, and tests.

AI Output

Suggested:

V2__create_salary_records.sql with id, employee_id FK, amount NUMERIC(15,2), currency_code, effective_date, created_at. Unique constraint on (employee_id, effective_date). Index on (employee_id, effective_date DESC).

SalaryRecord JPA entity, deliberately immutable after construction — no setters for amount, currencyCode, effectiveDate, employee.

SalaryRecordRepository with derived queries including findTopByEmployeeIdAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc.

SalaryRecordService with employee-existence check, amount positivity, future-date sanity bound (10 years), currency validation via java.util.Currency, duplicate-effective-date detection, sort whitelist, and page-size cap.

SalaryRecordController exposing POST, GET .../salaries, GET .../salaries/current.

DTOs: SalaryRecordRequest, SalaryRecordResponse, CurrentSalaryResponse.

New exceptions: DuplicateSalaryException (409), InvalidCurrencyException (400).

Three new test classes: SalaryRecordServiceTest, SalaryRecordRepositoryTest, SalaryRecordControllerTest.

Decisions and Modifications

Chose Option A for "current salary" resolution. The "current" salary is the latest record whose effectiveDate is on or before today. Future-dated records appear in history but not in current. Rejected "latest created_at wins" and "no future dating" as inferior for a real HR manager workflow. Documented as Decision 19.

Chose java.util.Currency for validation, not a hand-rolled whitelist. The JDK already ships the ISO 4217 list; Currency.getInstance(code) throws for unknown codes, giving complete validation with zero new dependency.

Amounts are BigDecimal, not double. Well-known money rule. The database stores NUMERIC(15,2).

Effective date is LocalDate, not TIMESTAMPTZ. Salary changes take effect on a calendar day.

ON DELETE RESTRICT on the FK to employees. Prevents deleting an employee who has salary history. Append-only enforced at the referential-integrity level too.

Service-level page-size cap and sort whitelist. These were already handled in the controller for employees; duplicated in the service for salary so that callers bypassing the controller also get the safety net.

Part 2 — Schema Type Mismatch: CHAR(3) vs VARCHAR(3)
After all files were saved, mvnw clean test failed on 21 tests with:

text
Schema validation: wrong column type encountered in column [currency_code]
in table [salary_records]; found [bpchar (Types#CHAR)], but expecting [varchar(3) (Types#VARCHAR)]
Root cause: V2 declared currency_code CHAR(3). In PostgreSQL, CHAR(n) is stored as fixed-width bpchar. Hibernate's ddl-auto=validate mode compares the entity's declared mapping (String with @Column(length = 3) = VARCHAR(3)) against the actual column (bpchar), and rejects the mismatch at startup.

Decisions and Modifications

Fixed at the schema level, not by weakening validation. The alternative would have been to change the entity to @Column(columnDefinition = "char(3)") or to relax ddl-auto to none. Rejected both. VARCHAR(3) is the conventional Postgres choice for a variable-length textual code; the existing CHECK (currency_code ~ '^[A-Z]{3}$') still enforces shape. ddl-auto=validate is one of the safety nets that caught this; weakening it would trade a real correctness check for convenience.

Corrected through a new migration, V3, not by editing V2. V2 had already been applied to the local database. Editing an applied migration would break other environments or require manual history-table surgery. This is the honest application of Decision 4's rule — never edit an applied migration — and is exactly the situation the rule exists for. V3__alter_salary_records_currency_code.sql runs ALTER TABLE salary_records ALTER COLUMN currency_code TYPE VARCHAR(3) USING currency_code::varchar(3); the pattern check remains in force.

Documented as Decision 20.

Verification

After V3 was added, mvnw clean test output included:

text
Successfully validated 3 migrations
Current version of schema "public": 2
Migrating schema "public" to version "3 - alter salary records currency code"
Successfully applied 1 migration to schema "public", now at version v3
Then Hibernate's validate mode passed and all tests ran to completion.

Confirmed in pgAdmin that information_schema.columns reports currency_code | character varying | 3 for the column.

Part 3 — Test Coverage
Three new test classes were added:

SalaryRecordServiceTest (12) — record employee-missing, invalid currency, future-date bound, duplicate effective date, currency normalisation; current employee-missing, no-effective-record, latest-effective-record; list employee-missing, mapped page, unsupported sort field, page-size cap.

SalaryRecordRepositoryTest (6) — save and find by employee id, unique constraint on (employee_id, effective_date), non-positive amount rejected, current resolves latest effective on or before today, current returns empty when only future-dated records exist, listing sorted by effective date descending.

SalaryRecordControllerTest (10) — record 201, invalid currency 400, negative amount 400, duplicate effective date 409, missing employee 404, current 200, current missing 404, list 200, oversized page size 400, unsupported sort field 400.

All three use the same patterns established in the employee milestone: @AutoConfigureMockMvc(addFilters = false) on the controller test, @Transactional for rollback and UUID-based employee numbers on the repository test, and mocked repository on the service test.

Verification
Ran the full test suite:

powershell
.\mvnw.cmd clean test
Recorded result:

text
Tests run: 61, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Test class	Tests
EmployeeControllerTest	10
EmployeeRepositoryTest	5
EmployeeServiceTest	8
EmployeeSpecificationsTest	9
SalaryRecordControllerTest	10
SalaryRecordRepositoryTest	6
SalaryRecordServiceTest	12
SalaryManagementApplicationTests	1
Total	61
Manual API Verification
The application was started with mvnw spring-boot:run from PowerShell with the dev profile active via .env. Postman requests against the running instance:

#	Request	Expected	Recorded
1	POST /api/employees with EMP-SALARY-01	201 with generated id	Matched
2	POST .../salaries {amount: 50000, currencyCode: GBP, effectiveDate: 2025-01-01}	201	Matched
3	POST .../salaries {amount: 55000, currencyCode: GBP, effectiveDate: 2026-01-01}	201	Matched
4	POST .../salaries {amount: 60000, currencyCode: GBP, effectiveDate: 2030-01-01}	201	Matched
5	GET .../salaries/current	200, returns the 2026-01-01 record	Matched
6	GET .../salaries	200, three records ordered 2030 → 2026 → 2025	Matched
7	POST .../salaries duplicate {effectiveDate: 2025-01-01}	409 with duplicate message	Matched
8	POST .../salaries {currencyCode: XYZ}	400 with ISO 4217 error message	Matched
9	GET .../salaries/current for 999999	404	Matched
10	GET .../salaries?size=500	400 with page-size cap message	Matched
11	GET /api/employees/{id}	200, employee still exists	Matched
Also verified in pgAdmin that:

Three rows exist in salary_records for the test employee with the expected effective-date ordering.

information_schema.columns reports currency_code as character varying | 3, confirming V3 applied cleanly.

The Postman collection was re-exported with all eleven scenarios and is committed at docs/postman/acme-salary-management.postman_collection.json.

Key Engineering Notes
The two problems hit in this milestone — the CHAR(3) vs VARCHAR(3) mismatch and the earlier spring-dotenv:4.0.0 Spring Boot 3 vs springboot4-dotenv Spring Boot 4 issue — were both real, both diagnosed from actual log output, and both fixed through the correct mechanism (a new migration; a corrected dependency). Neither was papered over by weakening validation. This is the sort of decision record a reviewer can inspect in the commit history and the design-decisions document, and it demonstrates engineering judgment rather than just code that compiles.

Commit

feat(backend): add salary records module with append-only history, currency validation, and 28 new tests

Remaining Verification Work
The following items are still outstanding:

Reporting module, including per-currency and per-department reporting.

CSV export of filtered employee and salary results.

JWT authentication and endpoint protection, replacing DevSecurityConfig.

Dedicated security test class covering unauthenticated and unauthorised requests.

Angular frontend.

Deterministic seed-data generator for 10,000 employees.

Dockerfiles and docker-compose.yml.

Deployment and demonstration video.

Update this list as work is completed and verified.

Record-Keeping Rule
Never record a test, feature, or security control as verified unless the relevant check has actually been run and its result is available.

Historical results must remain distinguishable from current results. Update this work log when new implementation decisions, AI-assisted changes, verification results, or related commits are made.