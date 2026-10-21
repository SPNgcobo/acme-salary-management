package za.co.acme.salarymanagement.report;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import za.co.acme.salarymanagement.common.InvalidCurrencyException;
import za.co.acme.salarymanagement.report.dto.CurrencyTotal;
import za.co.acme.salarymanagement.report.dto.DashboardResponse;
import za.co.acme.salarymanagement.report.dto.DistributionBucket;
import za.co.acme.salarymanagement.report.dto.DistributionResponse;
import za.co.acme.salarymanagement.report.dto.SalaryAggregateResponse;
import za.co.acme.salarymanagement.report.dto.SalaryAggregateRow;
import za.co.acme.salarymanagement.report.dto.TopEarnerRow;
import za.co.acme.salarymanagement.report.dto.TopEarnersResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business rules for reporting. All aggregations are executed as native
 * queries in {@link ReportRepository}; this service validates inputs,
 * bounds numeric parameters, and maps raw {@code Object[]} projections to
 * typed DTOs.
 *
 * <p>Currency codes are validated with {@link Currency#getInstance(String)},
 * consistent with the salary module. The same {@code InvalidCurrencyException}
 * and 400-mapped handler apply here, so error responses are consistent
 * across employee, salary, and reporting endpoints.
 */
@Service
public class ReportService {

    /** Maximum allowed {@code limit} for the top-earners endpoint. */
    static final int MAX_TOP_EARNERS = 100;

    /** Maximum bucket count for the distribution endpoint. */
    static final int MAX_DISTRIBUTION_BUCKETS = 200;

    /** Minimum bucket size for the distribution endpoint. */
    static final BigDecimal MIN_BUCKET_SIZE = new BigDecimal("1.00");

    private final ReportRepository repository;

    public ReportService(ReportRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        LocalDate today = LocalDate.now();
        long totalEmployees = repository.countAllEmployees();
        long employeesWithSalary = repository.countEmployeesWithCurrentSalary(today);
        long countries = repository.countDistinctCountries();
        long departments = repository.countDistinctDepartments();

        List<CurrencyTotal> totals = new ArrayList<>();
        for (Object[] row : repository.salaryTotalsByCurrency(today)) {
            totals.add(new CurrencyTotal(
                    (String) row[0],
                    (BigDecimal) row[1],
                    ((Number) row[2]).longValue(),
                    (BigDecimal) row[3]
            ));
        }

        return new DashboardResponse(
                totalEmployees,
                employeesWithSalary,
                countries,
                departments,
                totals,
                OffsetDateTime.now()
        );
    }

    @Transactional(readOnly = true)
    public SalaryAggregateResponse salaryByCountry(String currencyCode) {
        String currency = normaliseAndValidateCurrency(currencyCode);
        LocalDate today = LocalDate.now();
        List<SalaryAggregateRow> rows = mapAggregateRows(
                repository.salaryByCountry(today, currency)
        );
        return new SalaryAggregateResponse("country", currency, rows, OffsetDateTime.now());
    }

    @Transactional(readOnly = true)
    public SalaryAggregateResponse salaryByDepartment(String currencyCode) {
        String currency = normaliseAndValidateCurrency(currencyCode);
        LocalDate today = LocalDate.now();
        List<SalaryAggregateRow> rows = mapAggregateRows(
                repository.salaryByDepartment(today, currency)
        );
        return new SalaryAggregateResponse("department", currency, rows, OffsetDateTime.now());
    }

    @Transactional(readOnly = true)
    public DistributionResponse salaryDistribution(String currencyCode, BigDecimal bucketSize) {
        String currency = normaliseAndValidateCurrency(currencyCode);

        if (bucketSize == null) {
            throw new IllegalArgumentException("bucketSize is required");
        }
        if (bucketSize.compareTo(MIN_BUCKET_SIZE) < 0) {
            throw new IllegalArgumentException("bucketSize must be at least " + MIN_BUCKET_SIZE);
        }

        LocalDate today = LocalDate.now();
        BigDecimal max = repository.maxCurrentSalary(today, currency);
        if (max == null) {
            // No current salary in that currency: return an empty histogram
            // rather than throwing. This is not an error state; the caller
            // simply has no data to plot.
            return new DistributionResponse(currency, bucketSize, 0L, List.of(), OffsetDateTime.now());
        }

        long bucketCount = max.divide(bucketSize, 0, RoundingMode.CEILING).longValue();
        if (bucketCount < 1) {
            bucketCount = 1;
        }
        if (bucketCount > MAX_DISTRIBUTION_BUCKETS) {
            throw new IllegalArgumentException(
                    "resulting bucketCount would be " + bucketCount
                            + ", which exceeds the maximum of " + MAX_DISTRIBUTION_BUCKETS
                            + ". Increase bucketSize or reduce the salary range.");
        }

        List<DistributionBucket> buckets = new ArrayList<>();
        long total = 0L;
        for (Object[] row : repository.salaryDistribution(today, currency, bucketSize, bucketCount)) {
            long count = ((Number) row[2]).longValue();
            total += count;
            buckets.add(new DistributionBucket(
                    (BigDecimal) row[0],
                    (BigDecimal) row[1],
                    count
            ));
        }

        return new DistributionResponse(currency, bucketSize, total, buckets, OffsetDateTime.now());
    }

    @Transactional(readOnly = true)
    public TopEarnersResponse topEarners(String currencyCode, int limit) {
        String currency = normaliseAndValidateCurrency(currencyCode);

        if (limit < 1) {
            throw new IllegalArgumentException("limit must be at least 1");
        }
        if (limit > MAX_TOP_EARNERS) {
            throw new IllegalArgumentException("limit must be at most " + MAX_TOP_EARNERS);
        }

        LocalDate today = LocalDate.now();
        List<TopEarnerRow> rows = new ArrayList<>();
        for (Object[] row : repository.topEarners(today, currency, limit)) {
            rows.add(new TopEarnerRow(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    (String) row[2],
                    (String) row[3],
                    (String) row[4],
                    (String) row[5],
                    (BigDecimal) row[6],
                    (String) row[7],
                    toLocalDate(row[8])
            ));
        }

        return new TopEarnersResponse(currency, limit, rows, OffsetDateTime.now());
    }

    // ---- helpers ----

    private List<SalaryAggregateRow> mapAggregateRows(List<Object[]> raw) {
        List<SalaryAggregateRow> rows = new ArrayList<>(raw.size());
        for (Object[] row : raw) {
            rows.add(new SalaryAggregateRow(
                    (String) row[0],
                    ((Number) row[1]).longValue(),
                    (BigDecimal) row[2],
                    (BigDecimal) row[3],
                    (BigDecimal) row[4]
            ));
        }
        return rows;
    }

    private String normaliseAndValidateCurrency(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new IllegalArgumentException("currency is required");
        }
        String code = rawCode.trim().toUpperCase();
        try {
            Currency.getInstance(code);
        } catch (IllegalArgumentException e) {
            throw new InvalidCurrencyException(code);
        }
        return code;
    }

    /**
     * Normalises the JDBC temporal value for an {@code effective_date} column
     * to {@link LocalDate}.
     *
     * <p>Hibernate 7 + PostgreSQL returns {@code java.time.LocalDate} when a
     * native query's DATE column is materialised as part of an
     * {@code Object[]} result. Some driver configurations still return
     * {@code java.sql.Date}. Both are accepted. Anything else — including
     * {@code null} — is treated as an unexpected contract violation and
     * raised explicitly rather than silently converted to null, because a
     * null effective date in the top-earners report would display a broken
     * salary history to the HR user.
     */
    private static LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        throw new IllegalStateException(
                "Unexpected JDBC type for DATE column: "
                        + (value == null ? "null" : value.getClass().getName()));
    }
}