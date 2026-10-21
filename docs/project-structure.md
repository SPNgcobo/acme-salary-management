# Project Structure

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
backend/src/main/java/za/co/acme/salarymanagement/
├── report/                      # Planned reporting module
└── security/                    # Planned dedicated security module

backend/Dockerfile               # Planned
frontend/Dockerfile              # Planned
docker-compose.yml               # Planned
scripts/seed-*                   # Planned seed-data scripts
```

> **Note:** `DevSecurityConfig.java` currently exists in the `common` package for local development. The planned `security` package is intended for the dedicated security implementation.

## 3. Backend Responsibilities

### Employee Module

**Package:** `za.co.acme.salarymanagement.employee`

Manages employee profiles and provides employee creation, retrieval, directory listing, search, filtering, pagination, and sorting.

Search and filtering use `EmployeeSpecifications`, which builds composable Spring Data JPA specification predicates. `Pageable` handles pagination and sorting.

### Salary Module

**Package:** `za.co.acme.salarymanagement.salary`

Manages employee salary records as historical entries. Each salary change creates a new record with its own effective date and currency.

The API exposes endpoints under `/api/employees/{employeeId}/salaries` for creating salary records, listing salary history, and retrieving the current salary.

Currency codes are validated against `java.util.Currency` using ISO 4217 currency codes.

**Append-only history:** The `SalaryRecord` entity does not expose setters for `amount`, `currencyCode`, `effectiveDate`, or `employee`. Salary changes are intended to be recorded as new entries rather than updates to historical records.

**Current-salary resolution:** The current salary is the latest record whose `effectiveDate` is on or before the current date. Future-dated records remain in salary history but are not treated as the current salary.

### Reporting Module

**Status:** Planned

Will provide dashboard statistics and currency-aware salary reports grouped by country and department.

Reports must not combine different currencies into a single monetary total unless an explicit exchange-rate methodology is defined.

### Security Module

**Status:** Dedicated module planned; development security configuration exists.

Will handle authentication and authorisation for sensitive operations.

Security controls must be appropriate for the intended deployment environment. The development configuration must not be used as production security.

### Common Module

**Package:** `za.co.acme.salarymanagement.common`

Provides shared API error responses, exception handling, and web configuration.

Key components include:

- `ApiErrorResponse.java`: Standard API error response structure.
- `GlobalExceptionHandler.java`: Centralised handling of validation, type-mismatch, conflict, and not-found errors.
- `DuplicateEmployeeException.java`: Employee conflict handling.
- `DuplicateSalaryException.java`: Salary record conflict handling.
- `InvalidCurrencyException.java`: Invalid currency handling.
- `EmployeeNotFoundException.java`: Employee not-found handling.
- `DevSecurityConfig.java`: Development-only security configuration.
- `WebConfig.java`: Web and Spring Data configuration.

The exception handler should return consistent HTTP status codes and error response formats.

## 4. Configuration Files

| File | Purpose |
|---|---|
| `backend/src/main/resources/application.properties` | Base Spring Boot configuration, datasource environment placeholders, Flyway, Hibernate validation, Actuator, and logging. |
| `backend/src/main/resources/application-dev.properties` | Development-specific configuration, including application logging. |
| `backend/src/test/resources/application-test.properties` | Test profile configuration, including database and schema validation settings. |
| `backend/.env` | Local environment variables. Git-ignored and must never be committed. |
| `.env.example` | Committed root-level template containing placeholder configuration values. |

### Base Configuration

`application.properties` provides shared configuration across profiles. It uses environment variable placeholders such as:

- `${DB_URL:...}`
- `${DB_USERNAME:...}`
- `${DB_PASSWORD:...}`

It does not set a default active Spring profile. Profile selection is external.

### Development Configuration

`application-dev.properties` contains development-specific overrides and is loaded when the `dev` profile is active.

The `dev` profile permits unauthenticated requests and disables CSRF protection through `DevSecurityConfig`. This configuration is strictly for local API testing.

### Test Configuration

`application-test.properties` configures the test profile used by test classes annotated with `@ActiveProfiles("test")`.

The current setup points to the `acme_salary_management` PostgreSQL database, enables Flyway, and configures Hibernate schema validation.

Tests that use this database must be isolated appropriately. Transaction rollback helps with isolation for transactional tests, but does not automatically roll back every possible database side effect.

### Environment Variables

The `springboot4-dotenv` library loads local environment values from `backend/.env` when the application is launched with the expected working directory and configuration.

- `.env.example` is stored at the repository root and contains placeholders.
- `backend/.env` contains real local values and must remain Git-ignored.
- `SPRING_PROFILES_ACTIVE=dev` is used for local development.
- Production must set `SPRING_PROFILES_ACTIVE=prod` through the hosting environment and supply secrets through its environment configuration.

Never commit passwords, JWT secrets, or other sensitive configuration.

## 5. Frontend Responsibilities

**Status:** Planned. The Angular application has not yet been created.

The planned frontend areas are:

| Area | Responsibility |
|---|---|
| Authentication | Sign-in and authentication screens. |
| Dashboard | Employee counts, country coverage, and salary statistics. |
| Employees | Employee directory, search, filters, pagination, and employee details. |
| Salaries | Salary updates and historical records. |
| Reports | Country and department reports, filtering, and CSV export. |
| Core | Application-wide services, route guards, and HTTP interceptors. |
| Shared | Reusable components and common models. |

The frontend structure and responsibilities should be updated as implementation progresses.

## 6. API Testing

The Postman collection is stored at:

```text
docs/postman/acme-salary-management.postman_collection.json
```

It covers the implemented employee endpoints and the salary endpoints, including:

- Employee creation and retrieval.
- Employee listing, search, filtering, and sorting.
- Request validation and invalid sort fields.
- Salary creation and history listing.
- Current-salary retrieval.
- Duplicate salary dates and invalid currency codes.
- Missing employees and oversized page requests.

The collection must be updated whenever API endpoints, request schemas, or response contracts change.

Requests should be verified against a running application using the development profile. The collection must not contain real credentials or secrets.

## 7. Implementation Rule

A file or feature is not considered implemented merely because it appears in this document. The relevant code must exist in the repository, and the functionality must be verified through appropriate tests or other documented checks.

Update this document in the same commit that adds, renames, or removes files.