package za.co.acme.salarymanagement.report.dto;

import java.math.BigDecimal;

/**
 * One row of a salary aggregate report — per country, per department, or
 * per any other grouping dimension the report uses. The grouping key is
 * {@code groupKey} (a country code or a department name). All amounts are
 * in the single currency selected by the caller; no row mixes currencies.
 */
public record SalaryAggregateRow(
        String groupKey,
        long employeeCount,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        BigDecimal averageAmount
) {
}