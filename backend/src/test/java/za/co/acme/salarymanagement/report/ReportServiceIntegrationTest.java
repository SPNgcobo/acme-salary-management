package za.co.acme.salarymanagement.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import za.co.acme.salarymanagement.employee.Employee;
import za.co.acme.salarymanagement.employee.EmployeeRepository;
import za.co.acme.salarymanagement.employee.EmploymentStatus;
import za.co.acme.salarymanagement.report.dto.TopEarnersResponse;
import za.co.acme.salarymanagement.salary.SalaryRecord;
import za.co.acme.salarymanagement.salary.SalaryRecordRepository;

/**
 * Integration test that exercises {@link ReportService} against real
 * PostgreSQL, real Hibernate, and the actual native queries in
 * {@link ReportRepository}.
 *
 * <p>The purpose is to close the gap that the mock-based
 * {@link ReportServiceTest} cannot close: whether the {@code Object[]}
 * values returned by the JDBC driver match what the service expects. The
 * original top-earners bug (a {@code ClassCastException} casting
 * {@code LocalDate} to {@code java.sql.Date}) was invisible to unit tests
 * because the mock fed the wrong type. Tests in this class run against the
 * real driver and would have caught it.
 *
 * <p>Every test seeds its own data inside a rolled-back transaction, and
 * scopes assertions to rows it created (prefix {@code R-}, distinct
 * currency codes). The shared test database may contain Postman data or
 * data from other tests; those must not affect assertions here.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
@Import(ReportService.class)
class ReportServiceIntegrationTest {

    /**
     * Currency code used by these tests. Chosen because it is a valid ISO
     * 4217 code (so {@code Currency.getInstance} accepts it) but is not
     * used by the seed data or by the Postman smoke tests, so rows created
     * here are isolated from all other data in the shared database.
     */
    private static final String ISOLATED_CURRENCY = "XDR";

    @Autowired
    private ReportService reportService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private SalaryRecordRepository salaryRepository;

    /**
     * Regression test for the original bug. Asserts that the effective date
     * round-trips from a real PostgreSQL DATE column, through Hibernate's
     * native-query Object[] materialisation, through ReportService's type
     * normalisation, and into the DTO — as a LocalDate with the correct
     * value.
     */
    @Test
    void topEarnersReturnsEffectiveDateAsLocalDateFromRealPostgres() {
        LocalDate effective = LocalDate.now().minusDays(10);
        Employee employee = persistEmployeeWithSalary(
                new BigDecimal("250000.00"), ISOLATED_CURRENCY, effective);

        TopEarnersResponse response = reportService.topEarners(ISOLATED_CURRENCY, 100);

        assertThat(response.currencyCode()).isEqualTo(ISOLATED_CURRENCY);
        assertThat(response.earners()).hasSize(1);
        assertThat(response.earners().get(0).employeeId()).isEqualTo(employee.getId());
        assertThat(response.earners().get(0).employeeNumber())
                .isEqualTo(employee.getEmployeeNumber());
        assertThat(response.earners().get(0).amount())
                .isEqualByComparingTo("250000.00");
        assertThat(response.earners().get(0).effectiveDate()).isEqualTo(effective);
    }

    /**
     * Verifies the current-salary rule end-to-end: a future-dated salary
     * record must not appear in the top-earners report. This exercises the
     * {@code DISTINCT ON} + {@code effective_date <= today} logic against
     * real SQL.
     */
    @Test
    void topEarnersIgnoresFutureDatedSalaryRecords() {
        LocalDate past = LocalDate.now().minusDays(10);
        LocalDate future = LocalDate.now().plusDays(60);
        Employee employee = persistEmployee();
        salaryRepository.save(new SalaryRecord(
                employee, new BigDecimal("100000.00"), ISOLATED_CURRENCY, past));
        salaryRepository.save(new SalaryRecord(
                employee, new BigDecimal("999999.00"), ISOLATED_CURRENCY, future));
        salaryRepository.flush();

        TopEarnersResponse response = reportService.topEarners(ISOLATED_CURRENCY, 100);

        assertThat(response.earners()).hasSize(1);
        assertThat(response.earners().get(0).amount())
                .as("future-dated 999999.00 must not appear; past 100000.00 must")
                .isEqualByComparingTo("100000.00");
        assertThat(response.earners().get(0).effectiveDate()).isEqualTo(past);
    }

    /**
     * Verifies that the top-earners report is filtered by currency: an
     * employee paid in a different currency must not appear even if their
     * amount is higher.
     */
    @Test
    void topEarnersFiltersByCurrency() {
        // Same test class seeds two employees: one in ISOLATED_CURRENCY and
        // one in a different (but valid) ISO code. Only the isolated one
        // should appear in the response.
        Employee inIsolation = persistEmployeeWithSalary(
                new BigDecimal("100000.00"), ISOLATED_CURRENCY, LocalDate.now().minusDays(10));
        persistEmployeeWithSalary(
                new BigDecimal("999999.00"), "GBP", LocalDate.now().minusDays(10));

        TopEarnersResponse response = reportService.topEarners(ISOLATED_CURRENCY, 100);

        assertThat(response.earners()).hasSize(1);
        assertThat(response.earners().get(0).employeeId()).isEqualTo(inIsolation.getId());
    }

    /**
     * Verifies the dashboard's per-currency totals path. This exercises the
     * other native query that uses the same {@code current_salaries} CTE
     * pattern, so the fix covers more than just top-earners.
     */
    @Test
    void dashboardIncludesIsolatedCurrencyTotals() {
        LocalDate effective = LocalDate.now().minusDays(10);
        persistEmployeeWithSalary(new BigDecimal("40000.00"), ISOLATED_CURRENCY, effective);
        persistEmployeeWithSalary(new BigDecimal("60000.00"), ISOLATED_CURRENCY, effective);

        var dashboard = reportService.dashboard();

        assertThat(dashboard.salaryTotalsByCurrency())
                .filteredOn(ct -> ISOLATED_CURRENCY.equals(ct.currencyCode()))
                .singleElement()
                .satisfies(ct -> {
                    assertThat(ct.totalAmount()).isEqualByComparingTo("100000.00");
                    assertThat(ct.employeeCount()).isEqualTo(2L);
                });
    }

    /**
     * Verifies the distribution histogram path. Uses a bucket size large
     * enough that both salaries land in the same bucket, and asserts the
     * total matches. This exercises the {@code width_bucket} query.
     */
    @Test
    void salaryDistributionCoversIsolatedCurrencyRows() {
        LocalDate effective = LocalDate.now().minusDays(10);
        persistEmployeeWithSalary(new BigDecimal("42000.00"), ISOLATED_CURRENCY, effective);
        persistEmployeeWithSalary(new BigDecimal("48000.00"), ISOLATED_CURRENCY, effective);

        var response = reportService.salaryDistribution(
                ISOLATED_CURRENCY, new BigDecimal("100000.00"));

        assertThat(response.totalEmployeesInCurrency()).isEqualTo(2L);
        assertThat(response.buckets()).hasSize(1);
        assertThat(response.buckets().get(0).employeeCount()).isEqualTo(2L);
    }

    // ---- helpers ----

    private Employee persistEmployee() {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String employeeNumber = "R-" + suffix;
        return employeeRepository.save(new Employee(
                employeeNumber,
                "Test",
                "Report" + suffix,
                "report-" + suffix.toLowerCase() + "@test.acme",
                "GB",
                "Engineering",
                EmploymentStatus.ACTIVE
        ));
    }

    private Employee persistEmployeeWithSalary(
            BigDecimal amount, String currency, LocalDate effective) {
        Employee employee = persistEmployee();
        salaryRepository.save(new SalaryRecord(employee, amount, currency, effective));
        salaryRepository.flush();
        return employee;
    }
}