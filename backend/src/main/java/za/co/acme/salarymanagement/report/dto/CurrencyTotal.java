package za.co.acme.salarymanagement.report.dto;

import java.math.BigDecimal;

/**
 * Per-currency salary totals for the dashboard.
 *
 * <p>Reports do not sum across currencies (see Decision 10). The dashboard
 * exposes one of these per currency present in the currently-effective
 * salary data. A client that wants a single "total payroll" number must
 * choose a currency or apply an FX rule; the API never invents one.
 */
public record CurrencyTotal(
        String currencyCode,
        BigDecimal totalAmount,
        long employeeCount,
        BigDecimal averageAmount
) {
}