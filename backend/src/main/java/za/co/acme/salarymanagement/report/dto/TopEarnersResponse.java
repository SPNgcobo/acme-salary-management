package za.co.acme.salarymanagement.report.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Top-earners report response. Scoped to a single currency per request.
 * Ranking across currencies is intentionally not supported — a 50000 GBP
 * salary and a 60000 USD salary are not directly comparable, and any
 * attempt to rank them without an FX rule would be misleading.
 */
public record TopEarnersResponse(
        String currencyCode,
        int limit,
        List<TopEarnerRow> earners,
        OffsetDateTime generatedAt
) {
}