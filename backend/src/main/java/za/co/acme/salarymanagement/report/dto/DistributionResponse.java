package za.co.acme.salarymanagement.report.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Salary distribution histogram for a single currency.
 *
 * <p>{@code totalEmployeesInCurrency} is the sum of all bucket counts and
 * is provided so the caller can sanity-check the histogram without
 * iterating. The last bucket's {@code upperBoundExclusive} is the smallest
 * multiple of {@code bucketSize} that is greater than the maximum current
 * salary in that currency.
 */
public record DistributionResponse(
        String currencyCode,
        BigDecimal bucketSize,
        long totalEmployeesInCurrency,
        List<DistributionBucket> buckets,
        OffsetDateTime generatedAt
) {
}