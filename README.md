# ACME Salary Management

A web application for the ACME HR Manager to manage salaries for 10,000 employees across multiple countries, replacing spreadsheet-based salary management.

## Status

Work in progress — assessment submission for Incubyte.

## Tech Stack

- **Backend:** Spring Boot 4.1.1, Java 21, Spring Data JPA, Hibernate, Flyway, PostgreSQL 17, Spring Security
- **Frontend:** Angular (in progress)
- **Build:** Maven (Maven Wrapper included)
- **Testing:** JUnit 5, Spring Boot Test, Mockito

## Project Structure

- `backend/` — REST API and persistence
- `frontend/` — Angular UI (in progress)
- `docs/` — Requirements, architecture, design decisions, project structure, and AI work log
- `scripts/` — Seed data and utility scripts

## Prerequisites

- Java 21
- Node.js 20 or later (for the frontend, when implemented)
- PostgreSQL 17 running locally
- A database named `acme_salary_management`
- A PostgreSQL role named `acme_app` with the required database and schema permissions

## Local Setup

### 1. Configure environment variables

From the repository root, copy the environment template:

**Windows PowerShell:**

```powershell
Copy-Item .env.example .env
```

**Linux/macOS or Git Bash:**

```bash
cp .env.example .env
```

Update `.env` with your local database credentials and other required configuration values. Never commit your actual `.env` file.

### 2. Create the database and role

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

Grant the application role the required permissions:

```sql
GRANT CONNECT ON DATABASE acme_salary_management TO acme_app;

\connect acme_salary_management

GRANT USAGE, CREATE ON SCHEMA public TO acme_app;

GRANT SELECT, INSERT, UPDATE, DELETE
    ON ALL TABLES IN SCHEMA public TO acme_app;

GRANT USAGE, SELECT
    ON ALL SEQUENCES IN SCHEMA public TO acme_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO acme_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO acme_app;
```

**Note:** Default privileges apply to objects created by the role that executes `ALTER DEFAULT PRIVILEGES`. If Flyway migrations run under a different database role, configure the appropriate default privileges for that role as well.

### 3. Configure environment variables in PowerShell

From the repository root, set the environment variables in your current PowerShell session. Replace the example values with your local configuration.

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/acme_salary_management"
$env:DB_USERNAME = "acme_app"
$env:DB_PASSWORD = Read-Host "Enter your acme_app database password"
```

If the application requires JWT configuration, set these values too:

```powershell
$env:JWT_SECRET = "dev-only-secret-at-least-32-chars-long-1234567890"
$env:JWT_EXPIRATION_MS = "86400000"
```

Use a securely generated secret for local testing, and never commit real secrets. Only configure JWT variables if they are required by the current application configuration.

These environment variables remain available only in the current PowerShell session and its child processes.

### 4. Build and test the backend

From the repository root:

```powershell
cd backend
.\mvnw.cmd clean test
```

The Maven Wrapper downloads the required Maven distribution if necessary and runs the tests.

### 5. Run the backend

After the tests pass, run:

```powershell
.\mvnw.cmd spring-boot:run
```

The API is expected to listen at:

`http://localhost:8080`

This address assumes the application is configured to use port `8080`.

## Documentation

- `docs/requirements.md` — Scope, features, and non-functional requirements
- `docs/architecture.md` — Architecture, module responsibilities, data flow, and current status
- `docs/design-decisions.md` — Technical decisions, reasoning, and trade-offs
- `docs/project-structure.md` — Repository layout, including implemented and planned components
- `docs/ai-worklog.md` — AI usage log and verification results

## Security Notes

- `.env` is Git-ignored and stores local credentials. Never commit it.
- Only `.env.example` should be committed, containing placeholder values rather than real credentials.
- Do not commit database passwords, JWT secrets, or other sensitive configuration.
- Use least-privilege database permissions appropriate to the application's actual requirements.

## Database Migrations

Flyway manages database schema migrations when enabled and configured in the application.

Migration scripts are located at:

`backend/src/main/resources/db/migration/`

Migration files should be versioned and committed alongside the application changes they support.
