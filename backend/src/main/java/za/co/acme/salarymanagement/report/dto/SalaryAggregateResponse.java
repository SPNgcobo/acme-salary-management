package za.co.acme.salarymanagement.report.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Response shape for "salary by country" and "salary by department".
 *
 * <p>The endpoint is scoped to a single currency per request. The caller
 * chooses the currency; the response contains only that currency's figures
 * (Decision 10). If a client wants all currencies, it calls once per
 * currency it wants to display.
 */
public record SalaryAggregateResponse(
        String groupingDimension,
        String currencyCode,
        List<SalaryAggregateRow> rows,
        OffsetDateTime generatedAt
) {
}