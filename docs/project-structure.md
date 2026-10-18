# Project Structure

> **Status:** Living document. Update whenever files are added, renamed, or removed. Any file marked `(planned)` does not yet exist in the repository.

## Implemented

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
│   └── ai-worklog.md
├── backend/
│   ├── .mvn/
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/za/co/acme/salarymanagement/
│       │   │   ├── SalaryManagementApplication.java
│       │   │   └── employee/
│       │   │       ├── Employee.java
│       │   │       ├── EmployeeRepository.java
│       │   │       └── EmploymentStatus.java
│       │   └── resources/
│       │       ├── application.properties
│       │       └── db/migration/
│       │           └── V1__create_employees.sql
│       └── test/
│           ├── java/za/co/acme/salarymanagement/
│           │   ├── SalaryManagementApplicationTests.java
│           │   └── employee/
│           │       └── EmployeeRepositoryTest.java
│           └── resources/
│               └── application-test.properties
├── frontend/                 # Empty; created but no application yet
└── scripts/                  # Empty
```

## Planned (Not Yet Created)

The following files, packages, and configuration files are planned but do not yet exist in the repository.

```text
backend/src/main/java/za/co/acme/salarymanagement/
├── common/
│   ├── ApiErrorResponse.java                 (planned)
│   └── GlobalExceptionHandler.java           (planned)
├── employee/
│   ├── EmployeeService.java                  (planned)
│   ├── EmployeeController.java               (planned)
│   ├── EmployeeSpecifications.java            (planned)
│   └── dto/
│       ├── EmployeeRequest.java               (planned)
│       └── EmployeeResponse.java              (planned)
├── salary/                                   (planned)
├── report/                                   (planned)
└── security/                                 (planned)

backend/src/main/resources/db/migration/
└── V2__create_salary_records.sql              (planned)

backend/Dockerfile                             (planned)
frontend/Dockerfile                            (planned)
docker-compose.yml                             (planned)
scripts/seed.*                                 (planned)
```

## Backend Responsibilities

- **Employee module:** Employee profiles, creation and retrieval, directory listing, search, filtering, pagination, and sorting.
- **Salary module:** Salary amounts, currencies, effective dates, and append-only salary history. Updates must not overwrite previous salary records.
- **Reporting module:** Dashboard statistics and currency-aware salary reports grouped by country and department.
- **Security module:** Authentication and authorisation for sensitive operations.
- **Common module:** Shared API error responses and exception handling.

## Frontend Responsibilities (Planned)

The Angular application is planned to contain the following areas:

- **Authentication:** Sign-in and authentication screens.
- **Dashboard:** Employee counts, country coverage, and salary statistics.
- **Employees:** Employee directory, search, filters, pagination, and employee detail views.
- **Salaries:** Salary updates and historical records.
- **Reports:** Country and department reports, filtering, and CSV export.
- **Core:** Application-wide services, route guards, and HTTP interceptors.
- **Shared:** Reusable components and common models.

The frontend structure will be confirmed when the Angular application is generated.

## Implementation Rule

A planned file or feature is not considered implemented until it exists in the repository and its functionality has been verified.

This document must be updated in the same commit that adds, renames, or removes files, so the documented structure remains consistent with the actual repository.
