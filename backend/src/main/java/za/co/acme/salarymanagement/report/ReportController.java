package za.co.acme.salarymanagement.report;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.co.acme.salarymanagement.report.dto.DashboardResponse;
import za.co.acme.salarymanagement.report.dto.DistributionResponse;
import za.co.acme.salarymanagement.report.dto.SalaryAggregateResponse;
import za.co.acme.salarymanagement.report.dto.TopEarnersResponse;

/**
 * Read-only reporting endpoints for the HR Manager.
 *
 * <p>All endpoints are GET, all return JSON, and none mutate state. Every
 * response respects Decision 10: no cross-currency aggregation is ever
 * performed. Endpoints that aggregate salaries per employee require the
 * caller to specify one currency; the response contains only that
 * currency's figures.
 *
 * <p>Method-level parameter validation is enabled by {@code @Validated} on
 * the class. Failures raise {@code ConstraintViolationException} and are
 * translated to HTTP 400 by {@code GlobalExceptionHandler}, consistent with
 * the employee and salary endpoints.
 */
@RestController
@RequestMapping("/api/reports")
@Validated
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    /**
     * Headline numbers for the HR landing page: total employees, countries,
     * departments, and per-currency salary totals. Deliberately no single
     * "total payroll" field — see Decision 10.
     */
    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return service.dashboard();
    }

    /**
     * Salary aggregates grouped by country, in a single currency.
     *
     * <p>{@code currency} is required. Validated by the service against
     * {@code java.util.Currency}; an unknown code returns 400.
     */
    @GetMapping("/salary-by-country")
    public SalaryAggregateResponse salaryByCountry(
            @RequestParam @NotNull String currency
    ) {
        return service.salaryByCountry(currency);
    }

    /**
     * Salary aggregates grouped by department, in a single currency.
     *
     * <p>{@code currency} is required; see {@link #salaryByCountry(String)}.
     */
    @GetMapping("/salary-by-department")
    public SalaryAggregateResponse salaryByDepartment(
            @RequestParam @NotNull String currency
    ) {
        return service.salaryByDepartment(currency);
    }

    /**
     * Salary distribution histogram for one currency.
     *
     * <p>{@code bucketSize} is the width of each histogram bucket, in the
     * same currency. Validated by the service: must be at least 1.00 and
     * must produce no more than 200 buckets; otherwise 400.
     */
    @GetMapping("/salary-distribution")
    public DistributionResponse salaryDistribution(
            @RequestParam @NotNull String currency,
            @RequestParam @NotNull BigDecimal bucketSize
    ) {
        return service.salaryDistribution(currency, bucketSize);
    }

    /**
     * Top-earners report for one currency.
     *
     * <p>{@code limit} is the number of employees to return. Defaults to 10;
     * capped at 100 by the service. Values outside 1–100 return 400.
     */
    @GetMapping("/top-earners")
    public TopEarnersResponse topEarners(
            @RequestParam @NotNull String currency,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        return service.topEarners(currency, limit);
    }
}