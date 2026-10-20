# Project Structure

> **Status:** Living document. Update this file whenever files are added, renamed, or removed. Files or features marked **Planned** do not yet exist or are not yet implemented. This document distinguishes implemented components from planned work.

## 1. Implemented Repository Structure

The following tree documents the current repository structure as reported during development. Verify it against the actual repository before submission.

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
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/za/co/acme/salarymanagement/
│       │   │   ├── SalaryManagementApplication.java
│       │   │   ├── common/
│       │   │   │   ├── ApiErrorResponse.java
│       │   │   │   ├── DevSecurityConfig.java
│       │   │   │   ├── DuplicateEmployeeException.java
│       │   │   │   ├── EmployeeNotFoundException.java
│       │   │   │   ├── GlobalExceptionHandler.java
│       │   │   │   └── WebConfig.java
│       │   │   └── employee/
│       │   │       ├── Employee.java
│       │   │       ├── EmployeeController.java
│       │   │       ├── EmployeeRepository.java
│       │   │       ├── EmployeeSearchCriteria.java
│       │   │       ├── EmployeeService.java
│       │   │       ├── EmployeeSpecifications.java
│       │   │       ├── EmploymentStatus.java
│       │   │       └── dto/
│       │   │           ├── EmployeeRequest.java
│       │   │           └── EmployeeResponse.java
│       │   └── resources/
│       │       ├── application.properties
│       │       ├── application-dev.properties
│       │       └── db/migration/
│       │           └── V1__create_employees.sql
│       └── test/
│           ├── java/za/co/acme/salarymanagement/
│           │   ├── SalaryManagementApplicationTests.java
│           │   └── employee/
│           │       ├── EmployeeControllerTest.java
│           │       ├── EmployeeRepositoryTest.java
│           │       ├── EmployeeServiceTest.java
│           │       └── EmployeeSpecificationsTest.java
│           └── resources/
│               └── application-test.properties
├── frontend/                 # Empty; Angular application not created yet
└── scripts/                  # Empty
```

### Structure overview

| Directory or file | Responsibility |
|---|---|
| `backend/` | Spring Boot REST API, business logic, persistence, and backend tests |
| `frontend/` | Reserved for the planned Angular application |
| `docs/` | Requirements, architecture, design decisions, project structure, and AI work log |
| `docs/postman/` | Postman collection for API testing |
| `scripts/` | Reserved for seed-data generation and utility scripts |
| `.env.example` | Template for local environment configuration using placeholder values |
| `.gitignore` | Excludes local configuration, build output, and other files that should not be committed |

## 2. Planned Structure

The following components are planned and must not be treated as implemented until their code or configuration exists.

```text
backend/src/main/java/za/co/acme/salarymanagement/
├── salary/                         # Planned salary management module
├── report/                         # Planned reporting module
└── security/                       # Planned authentication and authorisation module

backend/src/main/resources/db/migration/
└── V2__create_salary_records.sql   # Planned salary history migration

backend/Dockerfile                  # Planned
frontend/Dockerfile                 # Planned
docker-compose.yml                  # Planned
scripts/seed.*                      # Planned seed-data generator
```

The exact files within these planned modules will be documented as implementation progresses.

## 3. Backend Responsibilities

### 3.1 Employee Module

**Location:** `backend/src/main/java/za/co/acme/salarymanagement/employee/`

Responsible for employee profile management, including:

- Creating and retrieving employee records.
- Listing employees through a directory endpoint.
- Searching and filtering employee records.
- Supporting pagination and sorting.
- Validating incoming data and handling duplicate or missing employees.

The reported implementation includes `EmployeeSpecifications` and `EmployeeSearchCriteria` for composing search criteria and database queries. Search, filtering, pagination, and sorting should be confirmed through automated tests and API requests.

### 3.2 Salary Module

**Status:** Planned

Responsible for:

- Managing salary amounts and currency codes.
- Recording salary effective dates.
- Maintaining append-only salary history.
- Preserving previous salary records when a new salary takes effect.

Salary changes must create new historical records rather than overwrite previous salary information.

### 3.3 Reporting Module

**Status:** Planned

Responsible for:

- Providing dashboard statistics.
- Producing salary reports grouped by country and department.
- Reporting amounts by currency without incorrectly combining different currencies.
- Supporting report filters and CSV export.

### 3.4 Security Module

**Status:** In progress; dedicated module and JWT implementation planned.

Responsible for authentication and authorisation for sensitive operations, including employee and salary data.

The development-only `DevSecurityConfig` is located in the `common/` package. It is intended for local testing and must not be treated as production security.

### 3.5 Common Module

**Location:** `backend/src/main/java/za/co/acme/salarymanagement/common/`

Contains shared application components, including:

- `ApiErrorResponse.java` — standard API error response structure.
- `DuplicateEmployeeException.java` — represents duplicate employee conflicts.
- `EmployeeNotFoundException.java` — represents employee lookup failures.
- `GlobalExceptionHandler.java` — centralises exception-to-HTTP-response handling.
- `WebConfig.java` — shared web and pagination configuration.
- `DevSecurityConfig.java` — development-profile security configuration.

The error handler should provide consistent responses for supported validation, type-mismatch, conflict, and not-found scenarios. Confirm these behaviours against the current tests.

## 4. Configuration Files

### `application.properties`

**Location:** `backend/src/main/resources/application.properties`

Contains base Spring Boot configuration shared across profiles.

The reported configuration includes:

- Database connection settings using environment variable placeholders such as `${DB_URL:...}`, `${DB_USERNAME:...}`, and `${DB_PASSWORD:...}`.
- Flyway migration settings.
- Hibernate schema validation.
- Actuator configuration.
- Logging configuration.

The base configuration does not select a default active profile. Profile selection is provided externally.

### `application-dev.properties`

**Location:** `backend/src/main/resources/application-dev.properties`

Contains development-only configuration, loaded when the `dev` profile is active. The reported purpose includes adjusting application logging.

Development-only settings must not weaken production security.

### `application-test.properties`

**Location:** `backend/src/test/resources/application-test.properties`

Contains configuration for tests that activate the `test` profile.

The documented setup uses PostgreSQL, Hibernate schema validation, and Flyway. Tests must be checked against the actual configuration to confirm which database they use and how test data is isolated.

If tests share a local database, use appropriate transaction rollback or other isolation mechanisms to prevent test data from affecting development data.

### `.env` and `.env.example`

The repository uses environment variables to configure database access and other environment-specific settings.

- **`.env.example`** is the committed template and must contain placeholder values only.
- **`.env`** contains local values and must remain excluded from version control.
- The reported local setup uses `backend/.env` when launching the backend from the `backend/` working directory.
- The `springboot4-dotenv` library is intended to load local environment variables. Confirm the dependency and its working-directory behaviour against the current `pom.xml` and application startup configuration.
- `SPRING_PROFILES_ACTIVE` can select the intended profile for local development. Production profile selection should be configured by the deployment environment.

Never include actual database passwords, JWT signing secrets, or other credentials in this document.

## 5. Frontend Responsibilities

**Status:** Planned. The Angular application has not yet been created.

The planned frontend will include the following areas:

| Area | Responsibility |
|---|---|
| Authentication | Sign-in and authentication screens |
| Dashboard | Employee counts, country coverage, and salary statistics |
| Employees | Employee directory, search, filters, pagination, and employee details |
| Salaries | Salary updates and historical salary records |
| Reports | Country and department reports, filters, and CSV export |
| Core | Application-wide services, route guards, and HTTP interceptors |
| Shared | Reusable components and common models |

The final frontend directory structure will be documented once the Angular application has been created.

## 6. API Testing

**Postman collection:** `docs/postman/acme-salary-management.postman_collection.json`

The reported collection covers employee API scenarios, including search, filtering, pagination, sorting, and validation errors.

Maintain the collection as endpoints and request or response contracts evolve.

API verification should include:

- Successful employee creation and retrieval.
- Invalid request data and validation errors.
- Duplicate employee handling.
- Missing employee handling.
- Search and filter combinations.
- Pagination limits and supported sort fields.
- Appropriate security behaviour for the active profile.

Run requests against the current application and record actual results. Document required environment variables and authentication steps without storing credentials or secrets in the collection.

## 7. Implementation and Documentation Rules

To keep this document accurate throughout development:

1. **Verify files:** List a file as implemented only when it exists in the repository.
2. **Verify functionality:** Mark a feature as verified only after its relevant tests or checks pass.
3. **Keep planned work separate:** Do not describe planned modules or features as completed.
4. **Update documentation with code:** Update this document in the same commit when repository structure changes.
5. **Record current test results:** Re-run relevant tests after changes and distinguish historical results from current results.
6. **Protect sensitive data:** Never commit real credentials, secrets, or personal employee data.
7. **Keep the tree current:** Add new files and remove or rename entries when the repository changes.

This document is a living reference for the repository layout, module responsibilities, configuration, and implementation progress of the ACME Salary Management System.
