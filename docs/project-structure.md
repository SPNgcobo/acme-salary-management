# Project Structure

> **Status:** Living document. Update whenever files are added, renamed, or
> removed. Any file marked `(planned)` does not yet exist in the repository.

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
├── backend/                # Spring Initializr scaffold only
│   ├── .mvn/
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/za/co/acme/salarymanagement/
│       │   │   └── SalaryManagementApplication.java
│       │   └── resources/
│       │       └── application.properties
│       └── test/
│           └── java/za/co/acme/salarymanagement/
│               └── SalaryManagementApplicationTests.java
├── frontend/               # empty, created but no app yet
└── scripts/                # empty
backend/src/main/java/za/co/acme/salarymanagement/
├── common/
│   ├── ApiErrorResponse.java                 (planned)
│   └── GlobalExceptionHandler.java           (planned)
├── employee/
│   ├── Employee.java                         (planned)
│   ├── EmployeeRepository.java               (planned)
│   ├── EmployeeService.java                  (planned)
│   ├── EmployeeController.java               (planned)
│   ├── EmployeeSpecifications.java           (planned)
│   └── dto/
│       ├── EmployeeRequest.java              (planned)
│       └── EmployeeResponse.java             (planned)
├── salary/                                   (planned)
├── report/                                   (planned)
└── security/                                 (planned)

backend/src/main/resources/db/migration/
├── V1__create_employees.sql                  (planned)
└── V2__create_salary_records.sql             (planned)

backend/Dockerfile                            (planned)
frontend/Dockerfile                           (planned)
docker-compose.yml                            (planned)
scripts/seed.*                                (planned)