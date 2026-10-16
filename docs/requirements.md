# ACME Salary Management — Requirements

## Goal
Replace spreadsheet-based salary management with a web application that lets
ACME's HR Manager manage employee and salary records for 10,000 employees
across multiple countries, and answer questions about how the organisation
pays people.

## Primary User
HR Manager. Handles sensitive data — access must be authenticated.

## Scope (v1)

- **Dashboard** — total employees, countries represented, salary totals,
  averages, and useful comparisons. Currency and reporting basis must be
  explicit.
- **Employee directory** — paginated table with search by name or employee
  number, and filters for country and department.
- **Salary management** — view and update salary, currency, and effective
  date. Validate inputs. Preserve salary history instead of overwriting.
- **Salary insights** — average by country/department, highest and lowest,
  employee counts, salary ranges.
- **Export** — export currently filtered employee and salary results to CSV
  with appropriate handling of sensitive information.
- **Seeding** — repeatable script generating 10,000 synthetic employees and
  salaries.
- **Delivery** — automated tests, incremental commits, deployed application,
  demo video, and design artifacts.

## Explicitly Out of Scope (with reasoning)

- **Payroll processing / payslips / tax calculations** — jurisdiction-specific
  and legally sensitive; a separate product.
- **Benefits administration, bank payments** — out of the stated problem.
- **Integrations with external HR systems** — no source system specified.
- **AI-generated salary recommendations** — no data to justify them and no
  stated need.
- **Bulk CSV import** — the migration from Excel is one-time; the seed script
  covers the demo need.
- **Multi-role permissions beyond HR Manager** — the assessment persona is a
  single role. Adding roles speculatively adds complexity without benefit.
- **Live FX rate integration** — introduces an external dependency,
  non-deterministic tests, and API key management. v1 reports per currency
  and leaves a pluggable FX service as future work.
- **i18n / localisation** — English-only UI in v1.

## Non-Functional Requirements

- **Performance:** server-side pagination and filtering; list endpoint
  responsive against 10k rows on an indexed table.
- **Correctness:** currencies never summed together without a documented
  conversion method.
- **Testability:** fast, deterministic tests over service and controller
  layers with synthetic data.
- **Security:** credentials never committed; salary data behind
  authentication.
- **Maintainability:** clear module boundaries and documented trade-offs.

## Success Criteria
An HR Manager can sign in, find any of 10,000 employees in seconds, view
salary history, record a new salary, export filtered results, and see
per-country and per-department figures — through a browser, against a
deployed backend.

## Delivery Constraints (from the assessment)
- Backend: Java / Spring Boot with a relational database.
- UI: Angular (Angular is preferred for the Java track).
- Seed script for 10,000 employees.
- Deployed software + demo video.
- Incremental Git history.
- Artifacts documenting thinking and AI usage.