# ACME Salary Management

A web application for the ACME HR Manager to manage employee records and salary information for 10,000 employees across multiple countries, replacing spreadsheet-based salary management.

## Status

**Work in progress:** Incubyte Software Craftsperson assessment.

The backend includes employee management, salary records, and reporting modules. The recorded test milestone is **105 passing tests against real PostgreSQL**. The Angular frontend, seed-data generator, and JWT security are still pending.

## Tech Stack

- **Backend:** Spring Boot 4.1.1, Java 21, Spring Data JPA, Hibernate, Flyway, PostgreSQL 17, Spring Security.
- **Frontend:** Angular (in progress).
- **Build:** Maven with Maven Wrapper.
- **Testing:** JUnit 5, Spring Boot Test, MockMvc, Mockito.
- **Local configuration:** `springboot4-dotenv`, configured to load environment variables from `backend/.env`.

## Project Structure

```text
acme-salary-management/
├── backend/          # Spring Boot REST API and persistence
├── frontend/         # Angular application (in progress)
├── docs/             # Requirements, architecture, decisions, and AI work log
├── scripts/          # Planned seed-data and utility scripts
├── .env.example      # Environment configuration template
├── .gitignore
└── README.md
```

For the detailed file tree, see [`docs/project-structure.md`](docs/project-structure.md).

## Prerequisites

Install or configure the following before running the backend:

- Java 21.
- PostgreSQL 17, running locally or accessible through the configured database URL.
- A database named `acme_salary_management`.
- A PostgreSQL role named `acme_app` with the required database and schema permissions.
- Node.js 20 or later when working on the frontend.

## Local Setup

### 1. Create the database and application role

Connect to PostgreSQL using `psql` as a database administrator or another appropriately privileged user.

Create the database:

```sql
CREATE DATABASE acme_salary_management;
```

Create the application role, replacing the placeholder with your own strong password:

```sql
CREATE ROLE acme_app
    WITH LOGIN PASSWORD 'YOUR_NEW_PASSWORD';
```

Grant the role access to the database:

```sql
GRANT CONNECT ON DATABASE acme_salary_management TO acme_app;
```

Connect to the database:

```sql
\connect acme_salary_management
```

Grant the role the required schema permissions:

```sql
GRANT USAGE, CREATE ON SCHEMA public TO acme_app;

GRANT SELECT, INSERT, UPDATE, DELETE
    ON ALL TABLES IN SCHEMA public TO acme_app;

GRANT USAGE, SELECT
    ON ALL SEQUENCES IN SCHEMA public TO acme_app;
```

Configure default privileges for tables and sequences created by the role executing these statements:

```sql
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO acme_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO acme_app;
```

**Important:** Default privileges apply to objects created by the role that executes `ALTER DEFAULT PRIVILEGES`. If Flyway migrations run under a different database role, configure the appropriate default privileges for that role as well.

Use database permissions appropriate to the application's actual requirements. In particular, grant schema creation privileges only to roles that need them.

### 2. Configure environment variables

The application uses `springboot4-dotenv` to load local environment values from `backend/.env`.

From the repository root, copy the committed template.

**Windows PowerShell:**

```powershell
Copy-Item .env.example backend/.env
```

**Linux, macOS, or Git Bash:**

```bash
cp .env.example backend/.env
```

Open `backend/.env` and configure the values:

| Variable | Purpose |
|---|---|
| `DB_URL` | JDBC URL for the PostgreSQL database. |
| `DB_USERNAME` | Database role, normally `acme_app`. |
| `DB_PASSWORD` | Password configured for the database role. |
| `JWT_SECRET` | Random secret of at least 32 characters, for the planned JWT implementation. |
| `JWT_EXPIRATION_MS` | Token lifetime in milliseconds; `86400000` represents 24 hours. |
| `SPRING_PROFILES_ACTIVE` | Use `dev` for local development. |

You can generate a random JWT secret with OpenSSL:

```bash
openssl rand -base64 48
```

Use the generated value for `JWT_SECRET` when configuring the future JWT implementation.

If a value contains `#`, `$`, or whitespace, wrap it in double quotes where required by the dotenv parser. For example:

```dotenv
DB_PASSWORD="your-password-with-special-characters"
```

An unquoted `#` may start a comment, and an unquoted `$` may trigger variable expansion. Check the dotenv parser's syntax if a value is not being loaded correctly.

**Security rules:**

- Never commit `backend/.env`.
- Commit only `.env.example`, containing placeholder values.
- Do not put real database passwords or JWT secrets in source code.
- Never activate the `dev` profile in production.

### 3. Build and test the backend

From the repository root, run:

```powershell
cd backend
.\mvnw.cmd clean test
```

The Maven Wrapper downloads the configured Maven distribution if necessary and runs the backend test suite.

The latest recorded milestone reports **105 tests passing**. This is a historical result; run the command above to verify the current code.

The tests use the test profile where configured by `@ActiveProfiles("test")`. Check `src/test/resources/application-test.properties` to confirm the database connection settings. The configured test database must be running and accessible.

### 4. Run the backend

From the `backend` directory, run:

```powershell
.\mvnw.cmd spring-boot:run
```

The startup output should indicate that the application started successfully. For example:

```text
The following 1 profile is active: "dev"
HikariPool-1 - Added connection ...
Tomcat started on port 8080
Started SalaryManagementApplication in X.XXX seconds
```

Exact log messages and startup times may differ.

The API base URL is:

[http://localhost:8080](http://localhost:8080)

### 5. Test the API with Postman

Import the Postman collection:

[`docs/postman/acme-salary-management.postman_collection.json`](docs/postman/acme-salary-management.postman_collection.json)

The collection contains 25 requests covering the implemented endpoints.

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/employees` | Create an employee. |
| `GET` | `/api/employees/{id}` | Retrieve an employee by ID. |
| `GET` | `/api/employees` | List employees with search, filters, pagination, and sorting. |
| `POST` | `/api/employees/{employeeId}/salaries` | Create a salary record. |
| `GET` | `/api/employees/{employeeId}/salaries` | Retrieve salary history. |
| `GET` | `/api/employees/{employeeId}/salaries/current` | Retrieve the current salary. |
| `GET` | `/api/reports/dashboard` | Retrieve dashboard headline numbers. |
| `GET` | `/api/reports/salary-by-country` | Retrieve salary aggregates per country, for one currency. |
| `GET` | `/api/reports/salary-by-department` | Retrieve salary aggregates per department, for one currency. |
| `GET` | `/api/reports/salary-distribution` | Retrieve a salary histogram for one currency. |
| `GET` | `/api/reports/top-earners` | Retrieve top earners for one currency. |

For local API testing, the `dev` profile permits unauthenticated requests and disables CSRF protection. This is a development-only configuration, not production security.

**Pagination response format**

This project uses `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)`. Paged responses follow this structure:

```json
{
  "content": [],
  "page": {
    "size": 20,
    "number": 0,
    "totalElements": 0,
    "totalPages": 0
  }
}
```

Use `body.page.totalElements` when asserting the total number of results in Postman or tests, rather than `body.totalElements`.

## Documentation

The `docs/` directory contains the project's supporting documentation.

| File | Description |
|---|---|
| [`docs/requirements.md`](docs/requirements.md) | Scope, features, and non-functional requirements. |
| [`docs/architecture.md`](docs/architecture.md) | Architecture, module responsibilities, data flow, and implementation status. |
| [`docs/design-decisions.md`](docs/design-decisions.md) | Technical decisions, reasoning, and trade-offs. |
| [`docs/project-structure.md`](docs/project-structure.md) | Repository layout, including implemented and planned components. |
| [`docs/ai-worklog.md`](docs/ai-worklog.md) | AI usage, decisions, and verification results. |

## Security Notes

- Keep `backend/.env` local and Git-ignored.
- Commit `.env.example` with placeholders only.
- Never commit database credentials, JWT secrets, or other sensitive configuration.
- Do not hardcode `SPRING_PROFILES_ACTIVE=dev` in committed configuration.
- The development security configuration disables CSRF and permits requests without authentication. Never use it as production security.
- Use least-privilege database permissions appropriate to the application's requirements.
- Configure production secrets and `SPRING_PROFILES_ACTIVE=prod` through the hosting platform's environment settings.
- Implement and verify JWT authentication and endpoint authorisation before exposing protected operations in production.

## Database Migrations

Flyway manages database schema migrations when enabled in the application configuration.

Migration scripts are located at:

```text
backend/src/main/resources/db/migration/
```

Migration files must be versioned and committed alongside the application changes they support.

Hibernate is configured to validate the database schema rather than automatically create or modify it. Keep entity definitions and Flyway migrations consistent. Once a migration has been applied, create a new migration for schema changes rather than editing the applied migration.

## Current Scope and Future Work

The employee, salary, and reporting modules provide the current backend foundation.

Outstanding work includes:

- [ ] CSV export of filtered employee and salary results.
- [ ] JWT authentication and endpoint protection, replacing `DevSecurityConfig`.
- [ ] Angular frontend.
- [ ] Deterministic seed-data generator for 10,000 employees.
- [ ] Dockerfiles and `docker-compose.yml`.
- [ ] Deployment and demonstration video.

Update this README as features are implemented and tested so that the documented status remains consistent with the codebase.
