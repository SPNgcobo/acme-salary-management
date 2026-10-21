# ACME Salary Management System: Project Structure

> **Status:** Living document. Update this file whenever files are added, renamed, or removed. Components marked **Planned** do not yet exist in the repository or have not yet been implemented.

## 1. Implemented Repository Structure

The following structure reflects the files and directories currently recorded as implemented.

```text
acme-salary-management/
├── .gitignore
├── .env.example
├── README.md
├── docs/
│   ├── requirements.md
│   ├── architecture.md
│   ├── design-decisions.md
│   ├── project-structure.md
│   ├── ai-worklog.md
│   └── postman/
│       └── acme-salary-management.postman_collection.json
├── backend/
│   ├── .mvn/
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── .env                     # Local only; Git-ignored
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/za/co/acme/salarymanagement/
│       │   │   ├── SalaryManagementApplication.java
│       │   │   ├── common/
│       │   │   │   ├── ApiErrorResponse.java
│       │   │   │   ├── DevSecurityConfig.java
│       │   │   │   ├── DuplicateEmployeeException.java
│       │   │   │   ├── DuplicateSalaryException.java
│       │   │   │   ├── EmployeeNotFoundException.java
│       │   │   │   ├── GlobalExceptionHandler.java
│       │   │   │   ├── InvalidCurrencyException.java
│       │   │   │   └── WebConfig.java
│       │   │   ├── employee/
│       │   │   │   ├── Employee.java
│       │   │   │   ├── EmployeeController.java
│       │   │   │   ├── EmployeeRepository.java
│       │   │   │   ├── EmployeeSearchCriteria.java
│       │   │   │   ├── EmployeeService.java
│       │   │   │   ├── EmployeeSpecifications.java
│       │   │   │   ├── EmploymentStatus.java
│       │   │   │   └── dto/
│       │   │   │       ├── EmployeeRequest.java
│       │   │   │       └── EmployeeResponse.java
│       │   │   ├── report/
│       │   │   │   ├── ReportController.java
│       │   │   │   ├── ReportRepository.java
│       │   │   │   ├── ReportService.java
│       │   │   │   └── dto/
│       │   │   │       ├── CurrencyTotal.java
│       │   │   │       ├── DashboardResponse.java
│       │   │   │       ├── DistributionBucket.java
│       │   │   │       ├── DistributionResponse.java
│       │   │   │       ├── SalaryAggregateResponse.java
│       │   │   │       ├── SalaryAggregateRow.java
│       │   │   │       ├── TopEarnerRow.java
│       │   │   │       └── TopEarnersResponse.java
│       │   │   └── salary/
│       │   │       ├── SalaryRecord.java
│       │   │       ├── SalaryRecordController.java
│       │   │       ├── SalaryRecordRepository.java
│       │   │       ├── SalaryRecordService.java
│       │   │       └── dto/
│       │   │           ├── CurrentSalaryResponse.java
│       │   │           ├── SalaryRecordRequest.java
│       │   │           └── SalaryRecordResponse.java
│       │   └── resources/
│       │       ├── application.properties
│       │       ├── application-dev.properties
│       │       └── db/migration/
│       │           ├── V1__create_employees.sql
│       │           ├── V2__create_salary_records.sql
│       │           └── V3__alter_salary_records_currency_code.sql
│       └── test/
│           ├── java/za/co/acme/salarymanagement/
│           │   ├── SalaryManagementApplicationTests.java
│           │   ├── employee/
│           │   │   ├── EmployeeControllerTest.java
│           │   │   ├── EmployeeRepositoryTest.java
│           │   │   ├── EmployeeServiceTest.java
│           │   │   └── EmployeeSpecificationsTest.java
│           │   ├── report/
│           │   │   ├── ReportControllerTest.java
│           │   │   ├── ReportRepositoryTest.java
│           │   │   ├── ReportServiceIntegrationTest.java
│           │   │   └── ReportServiceTest.java
│           │   └── salary/
│           │       ├── SalaryRecordControllerTest.java
│           │       ├── SalaryRecordRepositoryTest.java
│           │       └── SalaryRecordServiceTest.java
│           └── resources/
│               └── application-test.properties
├── frontend/                    # Empty; Angular application not created yet
└── scripts/                     # Empty
```

## 2. Planned Structure

The following components are planned but are not yet represented as implemented files.

```text
acme-salary-management/
├── backend/
│   ├── Dockerfile                # Planned
│   └── src/main/java/za/co/acme/salarymanagement/
│       └── security/             # Planned dedicated security module
├── frontend/
│   └── Dockerfile                # Planned
├── docker-compose.yml            # Planned
└── scripts/
    └── seed-*                    # Planned seed-data scripts
```

**Security note:** `DevSecurityConfig.java` currently exists in the `common` package and supports local development. The planned `security` package is intended for the dedicated security implementation.

## 3. Backend Responsibilities

### 3.1 Employee Module

**Package:** `za.co.acme.salarymanagement.employee`

Manages employee profiles and provides functionality for:

- Creating and retrieving employee records.
- Listing employees.
- Searching and filtering the employee directory.
- Pagination and sorting.

`EmployeeSpecifications` builds composable Spring Data JPA specification predicates for search and filtering. `Pageable` handles pagination and sorting.

### 3.2 Salary Module

**Package:** `za.co.acme.salarymanagement.salary`

Manages employee salary records as historical entries. Each salary change creates a new record with its own effective date and currency.

The API exposes endpoints under `/api/employees/{employeeId}/salaries` for:

- Creating salary records.
- Listing salary history.
- Retrieving the current salary.

Currency codes are validated using `java.util.Currency` and ISO 4217 currency codes.

**Append-only history:** The `SalaryRecord` entity does not expose setters for `amount`, `currencyCode`, `effectiveDate`, or `employee`. Salary changes are intended to be recorded as new entries rather than updates to historical records.

**Current-salary resolution:** The current salary is the latest record whose `effectiveDate` is on or before the current date. Future-dated records remain in salary history but are not treated as the current salary.

### 3.3 Reporting Module

**Package:** `za.co.acme.salarymanagement.report`

Provides dashboard statistics and currency-aware salary reports, including:

- Salary aggregates grouped by country and department.
- Salary distribution histograms.
- Top-earner lists.

Reporting queries use native SQL and PostgreSQL-specific features such as `DISTINCT ON` and `width_bucket`.

Each report request is scoped to a single currency. The system does not calculate combined totals across different currencies.

The service layer maps `Object[]` results from native queries to typed DTOs. This mapping is covered by mock-based unit tests and a real PostgreSQL integration test, `ReportServiceIntegrationTest`, as documented in [Design Decision 22](design-decisions.md).

### 3.4 Security Module

**Status:** Dedicated module planned; development security configuration exists.

The planned security module will handle authentication and authorisation for sensitive operations.

Security controls must be appropriate for the intended deployment environment. The development security configuration must not be used as production security.

### 3.5 Common Module

**Package:** `za.co.acme.salarymanagement.common`

Provides shared API error responses, exception handling, and web configuration.

| Component | Responsibility |
|---|---|
| `ApiErrorResponse.java` | Defines the standard API error response structure. |
| `GlobalExceptionHandler.java` | Centrally handles validation, type-mismatch, missing-parameter, conflict, and not-found errors. |
| `DuplicateEmployeeException.java` | Handles employee conflicts. |
| `DuplicateSalaryException.java` | Handles salary-record conflicts. |
| `InvalidCurrencyException.java` | Handles invalid currency codes. |
| `EmployeeNotFoundException.java` | Handles employee not-found errors. |
| `DevSecurityConfig.java` | Provides development-only security configuration. |
| `WebConfig.java` | Provides web and Spring Data configuration. |

The exception handler returns consistent HTTP status codes and error response formats for client-visible errors.

## 4. Configuration Files

### 4.1 Configuration Overview

| File | Purpose |
|---|---|
| `backend/src/main/resources/application.properties` | Base Spring Boot configuration, datasource environment placeholders, Flyway, Hibernate validation, Actuator, and logging. |
| `backend/src/main/resources/application-dev.properties` | Development-specific configuration, including application logging. |
| `backend/src/test/resources/application-test.properties` | Test profile configuration, including database and schema-validation settings. |
| `backend/.env` | Local environment variables. Git-ignored and must never be committed. |
| `.env.example` | Committed root-level template containing placeholder configuration values. |

### 4.2 Base Configuration

`application.properties` provides shared configuration across Spring profiles. It uses environment variable placeholders such as:

```properties
${DB_URL:...}
${DB_USERNAME:...}
${DB_PASSWORD:...}
```

The base configuration does not set a default active Spring profile. Profile selection is managed externally.

### 4.3 Development Configuration

`application-dev.properties` contains development-specific overrides and is loaded when the `dev` profile is active.

The development profile permits unauthenticated requests and disables CSRF protection through `DevSecurityConfig`. This configuration is strictly for local API testing.

**Do not use the development profile as production security.**

### 4.4 Test Configuration

`application-test.properties` configures the test profile used by test classes annotated with `@ActiveProfiles("test")`.

The current setup points to the `acme_salary_management` PostgreSQL database, enables Flyway, and configures Hibernate schema validation.

Tests using this database must be isolated appropriately.

- Transaction rollback helps isolate transactional tests, but it does not automatically roll back every possible database side effect.
- Test classes use unique identifiers, such as UUID-based employee numbers, to reduce cross-test collisions.
- Report integration tests use the distinctive currency code `XDR` to help prevent collisions with other test data.

### 4.5 Environment Variables

The `springboot4-dotenv` library loads local environment values from `backend/.env` when the application is launched with the expected working directory and configuration.

- `.env.example` is stored at the repository root and contains placeholders.
- `backend/.env` contains real local values and must remain Git-ignored.
- `SPRING_PROFILES_ACTIVE=dev` is used for local development.
- Production must set `SPRING_PROFILES_ACTIVE=prod` through the hosting environment and supply secrets through its environment configuration.

**Never commit passwords, JWT secrets, or other sensitive configuration.**

## 5. Frontend Responsibilities

**Status:** Planned. The Angular application has not yet been created.

The planned frontend areas are:

| Area | Responsibility |
|---|---|
| Authentication | Sign-in and authentication screens. |
| Dashboard | Employee counts, country coverage, and salary statistics. |
| Employees | Employee directory, search, filters, pagination, and employee details. |
| Salaries | Salary updates and historical records. |
| Reports | Country and department reports, filtering, distribution, top earners, and CSV export. |
| Core | Application-wide services, route guards, and HTTP interceptors. |
| Shared | Reusable components and common models. |

Update this section as the frontend structure and responsibilities are implemented.

## 6. API Testing

The Postman collection is stored at:

```text
docs/postman/acme-salary-management.postman_collection.json
```

It contains 25 requests covering the implemented employee, salary, and reporting endpoints.

### Employee endpoints

- Employee creation and retrieval.
- Employee listing, search, filtering, and sorting.
- Validation errors.

### Salary endpoints

- Salary creation.
- Salary history listing.
- Current-salary retrieval.
- Duplicate effective dates.
- Invalid currencies.
- Missing employees.
- Oversized page requests.

### Reporting endpoints

- Dashboard statistics.
- Salary aggregates by country.
- Salary aggregates by department.
- Salary distribution histogram.
- Top-earner list.
- Negative cases, including invalid currencies, missing required parameters, and oversized limits.

Update the Postman collection whenever API endpoints, request schemas, or response contracts change.

Requests should be verified against a running application using the development profile. The collection must not contain real credentials or secrets.

## 7. Implementation Rule

A file or feature is not considered implemented merely because it appears in this document. The relevant code must exist in the repository, and the functionality must be verified through appropriate tests or other documented checks.

Update this document in the same commit that adds, renames, or removes files.
