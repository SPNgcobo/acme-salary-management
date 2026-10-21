package za.co.acme.salarymanagement.report.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Headline numbers for the HR landing page.
 *
 * <p>{@code totalEmployees} counts every employee row regardless of salary
 * state. {@code employeesWithCurrentSalary} counts only those with at least
 * one salary record whose {@code effective_date} is on or before today.
 * The distinction is deliberate: an employee hired today with a future-dated
 * first salary still counts in headcount, but does not yet contribute to
 * payroll totals.
 *
 * <p>{@code salaryTotalsByCurrency} contains one entry per currency in the
 * current salary data. There is intentionally no single "total payroll"
 * field. See Decision 10.
 */
public record DashboardResponse(
        long totalEmployees,
        long employeesWithCurrentSalary,
        long countriesRepresented,
        long departmentsRepresented,
        List<CurrencyTotal> salaryTotalsByCurrency,
        OffsetDateTime generatedAt
) {
}