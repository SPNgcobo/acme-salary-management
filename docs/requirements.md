# ACME Salary Management System — Requirements

## 1. Project Overview

### Goal

Replace spreadsheet-based salary management with a web application that enables ACME's HR Manager to manage employee and salary records for **10,000 employees across multiple countries**.

The application must also provide useful salary insights to support HR reporting and decision-making.

### Primary User

**HR Manager**

The HR Manager manages employee information, records salary changes, and reviews salary statistics. Because the application handles sensitive employee and salary data, access must be authenticated and protected.

### Project Status

This document defines the requirements and agreed scope for version 1 (v1). A requirement being documented does not necessarily mean it has been implemented.

| Status | Meaning |
|---|---|
| Planned | Identified as part of the project scope |
| In Progress | Implementation has started |
| Implemented | Code has been written |
| Verified | Behaviour has been tested and confirmed |

---

## 2. Functional Requirements

The following requirements describe what the application must do.

### 2.1 Authentication and Security

- Allow the HR Manager to sign in securely.
- Protect API endpoints using JSON Web Tokens (JWT).
- Prevent unauthenticated users from accessing protected employee and salary data.
- Keep credentials, secrets, and environment-specific configuration out of version control.

**Initial status:** Planned

### 2.2 Dashboard

Provide an overview of the organisation, including:

- Total number of employees.
- Number of countries represented.
- Salary totals grouped by currency.
- Average salaries grouped by relevant dimensions.
- Useful comparisons across countries and departments.

All salary calculations must respect currency boundaries.

### 2.3 Employee Directory

Provide a paginated employee directory with:

- Search by employee name, employee number, and email address.
- Filtering by country, department, and employment status.
- Server-side pagination, searching, filtering, and sorting.
- Efficient retrieval of employee records without loading the entire dataset into the browser.

### 2.4 Employee Details and Salary History

For each employee, display:

- Employee profile information.
- Current or most recent salary information.
- Complete salary history, ordered from newest to oldest.

Salary history must remain available so that previous salary records are not lost when a new salary is recorded.

### 2.5 Salary Management

Allow the HR Manager to record a new salary with the following information:

- Employee.
- Salary amount.
- Currency.
- Effective date.

The application must validate incoming data and reject invalid salary records.

**Important business rule:** Salary history is append-only. Recording a new salary must create a new historical record rather than overwrite an existing salary record.

### 2.6 Salary Insights and Reporting

Provide salary statistics grouped by country and department, including:

- Average salary.
- Minimum salary.
- Maximum salary.
- Salary distribution histogram.
- Highest-paid employees.

**Currency rule:** Salary calculations and comparisons must be grouped by currency. Salaries in different currencies must never be added together or directly compared as if they had the same value.

### 2.7 CSV Export

Allow the HR Manager to export employee and salary results to CSV.

- Respect the filters applied to the current results.
- Export the relevant records rather than only the currently visible page, unless explicitly documented otherwise.
- Ensure exported data is consistent with the selected filters.

**Initial status:** Planned

### 2.8 Synthetic Data Seeding

Provide a repeatable script that generates synthetic data for:

- 10,000 employees.
- Corresponding salary records.
- Multiple countries and currencies.
- Relevant employee attributes needed for searching, filtering, and reporting.

The generated data must be suitable for development, testing, and demonstration without exposing real employee information.

**Initial status:** Planned

### 2.9 Delivery and Demonstration

The completed assessment must include:

- Automated tests.
- Incremental Git commits.
- A deployed application.
- A demonstration video.
- Documentation covering requirements, architecture, design decisions, project structure, and AI usage.

---

## 3. Explicitly Out of Scope

The following features are excluded from v1 to keep the implementation focused on the assessment requirements.

| Feature | Reason for Exclusion |
|---|---|
| Payroll processing, payslips, and tax calculations | These involve jurisdiction-specific rules and legal requirements and are outside the stated problem. |
| Benefits administration and bank payments | These are outside the required salary management workflow. |
| External HR system integrations | No source system or integration requirements have been specified. |
| AI-generated salary recommendations | There is no stated requirement or supporting data to justify automated recommendations. |
| Bulk CSV import | The Excel migration is treated as a one-time activity. Synthetic data seeding supports the demonstration requirement. |
| Multiple user roles and advanced permissions | The assessment specifies a single primary persona: the HR Manager. Additional roles would introduce unnecessary complexity. |
| Live foreign exchange (FX) rates | External FX services introduce dependencies, API key management, and less deterministic tests. V1 reports salary figures separately by currency. |
| Internationalisation and localisation (i18n) | The initial interface will be English-only. |

These exclusions may be reconsidered in a future version if new requirements justify them.

---

## 4. Non-Functional Requirements

Non-functional requirements describe the expected quality, security, and operational characteristics of the application.

### 4.1 Performance

- Support a dataset of 10,000 employees.
- Use server-side pagination, searching, filtering, and sorting.
- Use appropriate database indexes to support frequently queried fields.
- Keep employee listing and filtering responsive under the expected assessment workload.

### 4.2 Data Correctness and Integrity

- Never combine salary amounts across different currencies without a documented and valid conversion method.
- Preserve historical salary records when salaries change.
- Validate salary amounts, currencies, dates, and other required fields.
- Enforce appropriate data integrity constraints at the application and database levels.

### 4.3 Testability

- Provide automated tests for service, repository, and controller layers.
- Use synthetic data for repeatable and deterministic tests.
- Include integration tests against PostgreSQL where correctness depends on actual database behaviour.
- Test validation, error handling, pagination, filtering, and salary history rules.
- Ensure tests can be run consistently by another developer.

### 4.4 Security

- Require authentication for protected employee and salary endpoints.
- Never commit passwords, tokens, API keys, or other secrets.
- Validate and safely handle incoming requests.
- Restrict access to sensitive salary information.
- Return appropriate HTTP status codes and avoid exposing sensitive internal error details.

### 4.5 Maintainability

- Organise the application into clear, well-defined modules.
- Keep responsibilities separated across controllers, services, repositories, and other relevant components.
- Document significant design decisions and technical trade-offs.
- Follow consistent naming, formatting, and coding conventions.
- Maintain documentation as the implementation evolves.

### 4.6 Reliability and Repeatability

- Make the synthetic data seeding process repeatable.
- Use database migrations to manage schema changes.
- Keep tests independent of uncontrolled external services.
- Provide clear configuration instructions for local development and deployment.

---

## 5. Business Rules and Constraints

The following rules are central to the correctness of the system.

| ID | Rule |
|---|---|
| BR-01 | Salary records must be associated with the relevant employee. |
| BR-02 | Recording a salary change must preserve previous salary records. |
| BR-03 | Salary history must be displayed in descending order by effective date, with a deterministic tie-breaker where necessary. |
| BR-04 | Salary totals, averages, minimums, and maximums must be calculated within the relevant currency group. |
| BR-05 | Employee searching, filtering, pagination, and sorting must be performed server-side. |
| BR-06 | Protected employee and salary information must require authentication. |
| BR-07 | Development and demonstration datasets must use synthetic employee data. |
| BR-08 | CSV exports must respect the selected filters. |

---

## 6. Success Criteria

The project will be considered successful when the HR Manager can:

- Sign in to the application.
- Find any employee within a dataset of 10,000 employees.
- Search, filter, sort, and paginate employee records.
- View an employee's profile and full salary history.
- Record a new salary without losing historical records.
- Review salary statistics by country and department.
- View salary totals and comparisons without incorrectly mixing currencies.
- Export filtered employee and salary results to CSV.

The completed solution must be accessible through a browser, backed by a deployed backend, and supported by automated tests and the required assessment documentation.

---

## 7. Technical and Delivery Constraints

The implementation must comply with the following assessment constraints.

| Area | Requirement |
|---|---|
| Backend | Java and Spring Boot |
| Database | Relational database |
| Frontend | Angular, preferred for the Java assessment track |
| Dataset | Repeatable generation of 10,000 synthetic employees and salary records |
| Testing | Automated tests covering relevant application layers |
| Version control | Incremental Git history demonstrating the development process |
| Deployment | Deployed application suitable for demonstration |
| Demonstration | Demo video |
| Documentation | Requirements, architecture, design decisions, project structure, and AI usage |

---

## 8. Assumptions and Future Considerations

The following assumptions guide v1:

- The HR Manager is the primary user.
- Salary reporting is performed in each salary's recorded currency.
- Salary changes create new historical records.
- Synthetic data is sufficient for development and demonstration.
- External FX conversion is not required for the initial release.

Potential future enhancements include additional user roles, HR system integrations, internationalisation, and currency conversion through a configurable FX service.

These enhancements are not part of the current v1 scope.

---

## 9. Scope Management

Any proposed feature that changes the agreed v1 scope should be evaluated against:

1. The assessment requirements.
2. The benefit to the HR Manager.
3. Implementation complexity and delivery time.
4. Security and data integrity implications.
5. Testing and maintenance costs.

The primary objective is to deliver a secure, reliable, tested, and maintainable salary management application that satisfies the assessment requirements without introducing unnecessary complexity.
