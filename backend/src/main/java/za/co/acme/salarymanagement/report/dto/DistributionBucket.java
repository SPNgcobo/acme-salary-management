package za.co.acme.salarymanagement.report.dto;

import java.math.BigDecimal;

/**
 * One bucket of a salary-distribution report.
 *
 * <p>{@code lowerBoundInclusive} is included in the bucket, {@code
 * upperBoundExclusive} is not. A salary of exactly 10000.00 lands in the
 * bucket {@code [10000, 20000)}, not in {@code [0, 10000)}.
 */
public record DistributionBucket(
        BigDecimal lowerBoundInclusive,
        BigDecimal upperBoundExclusive,
        long employeeCount
) {
}