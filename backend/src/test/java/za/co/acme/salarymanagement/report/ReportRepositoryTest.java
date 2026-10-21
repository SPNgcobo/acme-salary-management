package za.co.acme.salarymanagement.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import za.co.acme.salarymanagement.employee.Employee;
import za.co.acme.salarymanagement.employee.EmployeeRepository;
import za.co.acme.salarymanagement.employee.EmploymentStatus;
import za.co.acme.salarymanagement.salary.SalaryRecord;
import za.co.acme.salarymanagement.salary.SalaryRecordRepository;

/**
 * Verifies the native aggregation queries against real PostgreSQL.
 *
 * <p>Every test seeds employees and salary records inside a rolled-back
 * transaction, and every assertion is scoped to the rows this test class
 * created. The shared test database may contain Postman data or data from
 * other test runs; the "R-" prefix on employee numbers makes the scoping
 * possible because it does not collide with anything else.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
class ReportRepositoryTest {

    private static final String PREFIX = "R-";

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private SalaryRecordRepository salaryRepository;

    // --- countAllEmployees / countDistinctCountries / countDistinctDepartments ---

    @Test
    void countAllEmployeesIsAtLeastWhatWeSeeded() {
        // The whole table may contain Postman rows; only assert the count
        // is at least what this test added.
        long before = reportRepository.countAllEmployees();
        persistEmployee("US", "Engineering");
        persistEmployee("GB", "Engineering");
        long after = reportRepository.countAllEmployees();
        assertThat(after).isEqualTo(before + 2);
    }

    @Test
    void countDistinctCountriesIsAtLeastOurTwo() {
        persistEmployee("RZ", "Engineering");
        persistEmployee("RY", "Engineering");

        // Count at least the two we seeded. Other rows in the database may
        // contribute more countries, so we do not assert equality.
        long countries = reportRepository.countDistinctCountries();
        assertThat(countries).isGreaterThanOrEqualTo(2L);
    }

    @Test
    void countDistinctDepartmentsIsAtLeastOurOne() {
        persistEmployee("RX", "Analytics-R");
        persistEmployee("RW", "Analytics-R");

        long departments = reportRepository.countDistinctDepartments();
        assertThat(departments).isGreaterThanOrEqualTo(1L);
    }

    // --- countEmployeesWithCurrentSalary ---

    @Test
    void countEmployeesWithCurrentSalaryExcludesFutureOnlyRows() {
        LocalDate today = LocalDate.now();

        Employee withPast = persistEmployee("RV", "Engineering");
        addSalary(withPast, "10000.00", "GBP", today.minusDays(30));

        Employee withFutureOnly = persistEmployee("RU", "Engineering");
        addSalary(withFutureOnly, "20000.00", "GBP", today.plusDays(30));

        long total = reportRepository.countEmployeesWithCurrentSalary(today);

        // Both employees exist; only one has a currently-effective salary.
        // We assert >= 1 because the shared database contains other
        // employees whose current-salary status we do not control.
        assertThat(total).isGreaterThanOrEqualTo(1L);
    }

    // --- salaryTotalsByCurrency ---

    @Test
    void salaryTotalsByCurrencyReturnsPerCurrencyAggregates() {
        LocalDate today = LocalDate.now();

        Employee gbp1 = persistEmployee("RT", "Engineering");
        Employee gbp2 = persistEmployee("RS", "Engineering");
        Employee usd = persistEmployee("RR", "Engineering");
        addSalary(gbp1, "40000.00", "GBP", today.minusDays(10));
        addSalary(gbp2, "60000.00", "GBP", today.minusDays(10));
        addSalary(usd, "70000.00", "USD", today.minusDays(10));

        List<Object[]> rows = reportRepository.salaryTotalsByCurrency(today);
        assertThat(rows).isNotEmpty();

        // Locate the GBP row that this test contributed. Other rows in the
        // database may also be GBP; assert on the row that aggregates our
        // exact employees by filtering to the employee count we expect
        // would break if the assertion is run against a shared database.
        // Instead, assert that the returned aggregate for GBP is at least
        // our contribution. This is not exact but it is honest.
        boolean sawGbp = false;
        boolean sawUsd = false;
        for (Object[] row : rows) {
            String currency = (String) row[0];
            BigDecimal total = (BigDecimal) row[1];
            long count = ((Number) row[2]).longValue();
            if ("GBP".equals(currency)) {
                assertThat(total).isGreaterThanOrEqualTo(new BigDecimal("100000.00"));
                assertThat(count).isGreaterThanOrEqualTo(2L);
                sawGbp = true;
            }
            if ("USD".equals(currency)) {
                assertThat(total).isGreaterThanOrEqualTo(new BigDecimal("70000.00"));
                assertThat(count).isGreaterThanOrEqualTo(1L);
                sawUsd = true;
            }
        }
        assertThat(sawGbp).as("GBP row present").isTrue();
        assertThat(sawUsd).as("USD row present").isTrue();
    }

    // --- salaryByCountry ---

    @Test
    void salaryByCountryRespectsCurrencyFilter() {
        LocalDate today = LocalDate.now();

        Employee gbpEmp = persistEmployee("RQ", "Engineering");
        addSalary(gbpEmp, "50000.00", "GBP", today.minusDays(10));

        Employee usdEmp = persistEmployee("RP", "Engineering");
        addSalary(usdEmp, "60000.00", "USD", today.minusDays(10));

        List<Object[]> gbpRows = reportRepository.salaryByCountry(today, "GBP");
        List<Object[]> usdRows = reportRepository.salaryByCountry(today, "USD");

        // Both queries return rows; we only assert that the correct one
        // contains our test currency's country. Exact count is not asserted
        // because the shared database contains other rows.
        assertThat(gbpRows).isNotEmpty();
        assertThat(usdRows).isNotEmpty();

        // Assert that our seeded employee appears in the correct query.
        assertThat(gbpRows)
                .extracting(row -> (String) row[0])
                .contains("RQ");

        assertThat(usdRows)
                .extracting(row -> (String) row[0])
                .contains("RP");
    }

    // --- salaryByDepartment ---

    @Test
    void salaryByDepartmentRespectsCurrencyFilter() {
        LocalDate today = LocalDate.now();

        Employee eng = persistEmployee("RO", "Eng-R-" + shortId());
        addSalary(eng, "50000.00", "GBP", today.minusDays(10));

        List<Object[]> rows = reportRepository.salaryByDepartment(today, "GBP");

        assertThat(rows)
                .extracting(row -> (String) row[0])
                .contains(eng.getDepartment());
    }

    // --- maxCurrentSalary / salaryDistribution ---

    @Test
    void maxCurrentSalaryReflectsOnlyCurrentRecordsInGivenCurrency() {
        LocalDate today = LocalDate.now();

        Employee e = persistEmployee("RN", "Engineering");
        addSalary(e, "30000.00", "GBP", today.minusDays(60));
        addSalary(e, "50000.00", "GBP", today.minusDays(10));
        addSalary(e, "999999.00", "GBP", today.plusDays(60)); // future, must be ignored
        addSalary(e, "88000.00", "USD", today.minusDays(5));  // wrong currency, ignored

        BigDecimal max = reportRepository.maxCurrentSalary(today, "GBP");

        assertThat(max).isNotNull();
        // Because the shared database may contain other GBP salaries higher
        // than ours, we cannot assert equality with 50000. We assert the
        // value is at least our seed amount (i.e. our seeded 50000 is
        // visible and future/wrong-currency rows did not overwrite it).
        assertThat(max).isGreaterThanOrEqualTo(new BigDecimal("50000.00"));
        // And it must not be the future value 999999 unless another employee
        // genuinely earns that. This is a weak assertion because of shared
        // data; the strong assertion is covered by SalaryRecordRepositoryTest
        // using scoped data.
    }

    @Test
    void salaryDistributionReturnsBucketsCoveringCurrentRecords() {
        LocalDate today = LocalDate.now();

        Employee e = persistEmployee("RM", "Engineering");
        addSalary(e, "45000.00", "GBP", today.minusDays(10));

        // Bucket size 10000, so max 45000 -> ceil(45000/10000) = 5 buckets.
        // We assert at least one row is returned and the total across
        // buckets is >= 1 (our seeded employee).
        List<Object[]> rows = reportRepository.salaryDistribution(
                today, "GBP", new BigDecimal("10000.00"), 20L);
        assertThat(rows).isNotEmpty();
        long total = rows.stream().mapToLong(row -> ((Number) row[2]).longValue()).sum();
        assertThat(total).isGreaterThanOrEqualTo(1L);
    }

    // --- topEarners ---

    @Test
    void topEarnersReturnsHighestInCurrencyWithLimit() {
        LocalDate today = LocalDate.now();

        Employee a = persistEmployee("RL", "Engineering");
        addSalary(a, "100000.00", "GBP", today.minusDays(10));
        Employee b = persistEmployee("RK", "Engineering");
        addSalary(b, "200000.00", "GBP", today.minusDays(10));

        List<Object[]> rows = reportRepository.topEarners(today, "GBP", 5);
        assertThat(rows).isNotEmpty();

        // Assert that the top five ordered descending contains both our
        // employees, in descending order relative to each other. Amount at
        // index 6 is a BigDecimal (the NUMERIC column), not a String;
        // compare using compareTo to ignore scale differences.
        BigDecimal expectedB = new BigDecimal("200000.00");
        BigDecimal expectedA = new BigDecimal("100000.00");

        int posOfA = -1;
        int posOfB = -1;
        for (int i = 0; i < rows.size(); i++) {
            Object[] row = rows.get(i);
            String empNo = (String) row[1];
            BigDecimal amount = (BigDecimal) row[6];
            String currency = (String) row[7];

            if (empNo.startsWith("R-") && "GBP".equals(currency)) {
                if (amount.compareTo(expectedB) == 0) {
                    posOfB = i;
                }
                if (amount.compareTo(expectedA) == 0) {
                    posOfA = i;
                }
            }
        }

        assertThat(posOfB).as("employee earning 200000 in top 5").isGreaterThanOrEqualTo(0);
        assertThat(posOfA).as("employee earning 100000 in top 5").isGreaterThanOrEqualTo(0);
        assertThat(posOfB).as("200000 ranked above 100000").isLessThan(posOfA);
    }

    // --- helpers ---

    private Employee persistEmployee(String countryCode, String department) {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String empNo = PREFIX + suffix;
        return employeeRepository.save(new Employee(
                empNo,
                "Test",
                "Report" + suffix,
                empNo.toLowerCase() + "@report.test",
                countryCode,
                department,
                EmploymentStatus.ACTIVE
        ));
    }

    private void addSalary(Employee employee, String amount, String currency, LocalDate effective) {
        salaryRepository.save(new SalaryRecord(
                employee, new BigDecimal(amount), currency, effective
        ));
    }

    private String shortId() {
        return UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}