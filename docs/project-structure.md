# Project Structure

> **Status:** Living document. Update this file whenever files are added, renamed, or removed. Any file marked `(planned)` does not yet exist in the repository.

## Implemented

The following structure represents the implemented repository layout. Confirm that each listed file exists before treating it as implemented.

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
│       │   │   │   ├── EmployeeNotFoundException.java
│       │   │   │   ├── GlobalExceptionHandler.java
│       │   │   │   └── WebConfig.java
│       │   │   └── employee/
│       │   │       ├── Employee.java
│       │   │       ├── EmployeeController.java
│       │   │       ├── EmployeeRepository.java
│       │   │       ├── EmployeeService.java
│       │   │       ├── EmploymentStatus.java
│       │   │       └── dto/
│       │   │           ├── EmployeeRequest.java
│       │   │           └── EmployeeResponse.java
│       │   └── resources/
│       │       ├── application.properties
│       │       └── db/migration/
│       │           └── V1__create_employees.sql
│       └── test/
│           ├── java/za/co/acme/salarymanagement/
│           │   ├── SalaryManagementApplicationTests.java
│           │   └── employee/
│           │       ├── EmployeeControllerTest.java
│           │       ├── EmployeeRepositoryTest.java
│           │       └── EmployeeServiceTest.java
│           └── resources/
│               └── application-test.properties
├── frontend/                 # Empty; Angular application not created yet
└── scripts/                  # Empty
```

## Planned (Not Yet Created)

The following files, packages, and configuration files are planned for future implementation. They must not be treated as existing or completed functionality.

```text
backend/src/main/java/za/co/acme/salarymanagement/
├── employee/
│   ├── EmployeeSpecifications.java             (planned)
│   └── Additional query and filtering code     (planned)
├── salary/                                     (planned)
├── report/                                     (planned)
└── security/                                   (planned)

backend/src/main/resources/db/migration/
└── V2__create_salary_records.sql                (planned)

backend/Dockerfile                               (planned)
frontend/Dockerfile                              (planned)
docker-compose.yml                               (planned)
scripts/seed.*                                   (planned)
```

## Backend Responsibilities

### Employee Module

Manages employee profiles, employee creation and retrieval, directory listing, search, filtering, pagination, and sorting.

### Salary Module (Planned)

Manages salary amounts, currencies, effective dates, and append-only salary history. Salary updates must preserve previous records rather than overwrite historical salary data.

### Reporting Module (Planned)

Provides dashboard statistics and currency-aware salary reports grouped by country and department.

### Security Module

Handles authentication and authorisation for sensitive operations. Security configuration must be appropriate for the intended deployment environment.

### Common Module

Provides shared API error responses, exception handling, and web configuration.

## Frontend Responsibilities (Planned)

The Angular application is planned to include the following areas.

- **Authentication:** Sign-in and authentication screens.
- **Dashboard:** Employee counts, country coverage, and salary statistics.
- **Employees:** Employee directory, search, filters, pagination, and employee detail views.
- **Salaries:** Salary updates and historical records.
- **Reports:** Country and department reports, filtering, and CSV export.
- **Core:** Application-wide services, route guards, and HTTP interceptors.
- **Shared:** Reusable components and common models.

The final frontend directory structure will be documented when the Angular application is created.

## API Testing

The Postman collection is stored at:

`docs/postman/acme-salary-management.postman_collection.json`

The collection should be maintained as API endpoints and request or response contracts evolve. Requests should be verified against the running application, and any required environment variables or authentication steps should be documented without including real credentials or secrets.

## Implementation Rule

A planned file or feature is not considered implemented until the relevant code exists in the repository and its functionality has been verified.

This document must be updated in the same commit that adds, renames, or removes files. Implemented and planned sections must accurately reflect the repository's current state.

