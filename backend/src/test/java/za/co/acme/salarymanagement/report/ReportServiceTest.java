package za.co.acme.salarymanagement.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.acme.salarymanagement.common.InvalidCurrencyException;
import za.co.acme.salarymanagement.report.dto.DashboardResponse;
import za.co.acme.salarymanagement.report.dto.DistributionResponse;
import za.co.acme.salarymanagement.report.dto.SalaryAggregateResponse;
import za.co.acme.salarymanagement.report.dto.TopEarnersResponse;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository repository;

    @InjectMocks
    private ReportService service;

    // --- dashboard ---

    @Test
    void dashboardAggregatesRepositoryCountsAndCurrencyTotals() {
        when(repository.countAllEmployees()).thenReturn(10000L);
        when(repository.countEmployeesWithCurrentSalary(any(LocalDate.class))).thenReturn(9500L);
        when(repository.countDistinctCountries()).thenReturn(12L);
        when(repository.countDistinctDepartments()).thenReturn(8L);
        when(repository.salaryTotalsByCurrency(any(LocalDate.class))).thenReturn(List.<Object[]>of(
                new Object[]{"GBP", new BigDecimal("250000000.00"), 3000L, new BigDecimal("83333.33")},
                new Object[]{"USD", new BigDecimal("180000000.00"), 2500L, new BigDecimal("72000.00")}
        ));

        DashboardResponse response = service.dashboard();

        assertThat(response.totalEmployees()).isEqualTo(10000L);
        assertThat(response.employeesWithCurrentSalary()).isEqualTo(9500L);
        assertThat(response.countriesRepresented()).isEqualTo(12L);
        assertThat(response.departmentsRepresented()).isEqualTo(8L);
        assertThat(response.salaryTotalsByCurrency()).hasSize(2);
        assertThat(response.salaryTotalsByCurrency().get(0).currencyCode()).isEqualTo("GBP");
        assertThat(response.salaryTotalsByCurrency().get(0).totalAmount())
                .isEqualByComparingTo("250000000.00");
    }

    // --- salaryByCountry / salaryByDepartment ---

    @Test
    void salaryByCountryRejectsUnknownCurrency() {
        assertThatThrownBy(() -> service.salaryByCountry("XYZ"))
                .isInstanceOf(InvalidCurrencyException.class)
                .hasMessageContaining("XYZ");
    }

    @Test
    void salaryByCountryNormalisesLowercase() {
        when(repository.salaryByCountry(any(LocalDate.class), eq("GBP")))
                .thenReturn(List.<Object[]>of(new Object[]{"GB", 10L,
                        new BigDecimal("30000.00"), new BigDecimal("90000.00"), new BigDecimal("60000.00")}));

        SalaryAggregateResponse response = service.salaryByCountry("gbp");

        assertThat(response.currencyCode()).isEqualTo("GBP");
        assertThat(response.groupingDimension()).isEqualTo("country");
        assertThat(response.rows()).hasSize(1);
        assertThat(response.rows().get(0).groupKey()).isEqualTo("GB");
    }

    @Test
    void salaryByDepartmentRejectsUnknownCurrency() {
        assertThatThrownBy(() -> service.salaryByDepartment("ZZZ"))
                .isInstanceOf(InvalidCurrencyException.class);
    }

    @Test
    void salaryByDepartmentReturnsMappedRows() {
        when(repository.salaryByDepartment(any(LocalDate.class), eq("USD")))
                .thenReturn(List.<Object[]>of(new Object[]{"Engineering", 20L,
                        new BigDecimal("50000.00"), new BigDecimal("200000.00"), new BigDecimal("100000.00")}));

        SalaryAggregateResponse response = service.salaryByDepartment("USD");

        assertThat(response.groupingDimension()).isEqualTo("department");
        assertThat(response.rows()).hasSize(1);
        assertThat(response.rows().get(0).groupKey()).isEqualTo("Engineering");
        assertThat(response.rows().get(0).employeeCount()).isEqualTo(20L);
    }

    // --- salaryDistribution ---

    @Test
    void salaryDistributionRejectsUnknownCurrency() {
        assertThatThrownBy(() -> service.salaryDistribution("XYZ", new BigDecimal("10000")))
                .isInstanceOf(InvalidCurrencyException.class);
    }

    @Test
    void salaryDistributionRejectsNullBucketSize() {
        assertThatThrownBy(() -> service.salaryDistribution("GBP", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bucketSize");
    }

    @Test
    void salaryDistributionRejectsTooSmallBucketSize() {
        assertThatThrownBy(() -> service.salaryDistribution("GBP", new BigDecimal("0.01")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least");
    }

    @Test
    void salaryDistributionReturnsEmptyWhenNoSalaries() {
        when(repository.maxCurrentSalary(any(LocalDate.class), eq("GBP"))).thenReturn(null);

        DistributionResponse response = service.salaryDistribution("GBP", new BigDecimal("10000"));

        assertThat(response.totalEmployeesInCurrency()).isZero();
        assertThat(response.buckets()).isEmpty();
    }

    @Test
    void salaryDistributionRejectsTooManyBuckets() {
        when(repository.maxCurrentSalary(any(LocalDate.class), eq("GBP")))
                .thenReturn(new BigDecimal("9999999.00"));

        assertThatThrownBy(() -> service.salaryDistribution("GBP", new BigDecimal("1000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds");
    }

    @Test
    void salaryDistributionMapsBucketsAndTotal() {
        when(repository.maxCurrentSalary(any(LocalDate.class), eq("GBP")))
                .thenReturn(new BigDecimal("45000.00"));
        when(repository.salaryDistribution(any(LocalDate.class), eq("GBP"),
                any(BigDecimal.class), anyLong())).thenReturn(List.<Object[]>of(
                new Object[]{new BigDecimal("40000.00"), new BigDecimal("50000.00"), 3L}
        ));

        DistributionResponse response = service.salaryDistribution("GBP", new BigDecimal("10000"));

        assertThat(response.totalEmployeesInCurrency()).isEqualTo(3L);
        assertThat(response.buckets()).hasSize(1);
        assertThat(response.buckets().get(0).lowerBoundInclusive())
                .isEqualByComparingTo("40000.00");
    }

    // --- topEarners ---

    @Test
    void topEarnersRejectsUnknownCurrency() {
        assertThatThrownBy(() -> service.topEarners("XYZ", 10))
                .isInstanceOf(InvalidCurrencyException.class);
    }

    @Test
    void topEarnersRejectsLimitBelowOne() {
        assertThatThrownBy(() -> service.topEarners("GBP", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 1");
    }

    @Test
    void topEarnersRejectsLimitAboveCap() {
        assertThatThrownBy(() -> service.topEarners("GBP", 500))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at most");
    }

    /**
     * Hibernate 7 + PostgreSQL returns {@link LocalDate} for a DATE column in
     * a native {@code Object[]} result. This is what production sees. Using
     * {@code java.sql.Date} here was the original bug: the mock fed a type
     * that never occurs at runtime, so the test passed while the endpoint
     * failed in production.
     */
    @Test
    void topEarnersMapsRowsWithLocalDateFromHibernate() {
        LocalDate effective = LocalDate.of(2025, 6, 1);
        when(repository.topEarners(any(LocalDate.class), eq("GBP"), eq(5)))
                .thenReturn(List.<Object[]>of(new Object[]{
                        42L, "EMP-00042", "Ada", "Lovelace",
                        "Engineering", "GB",
                        new BigDecimal("250000.00"), "GBP", effective
                }));

        TopEarnersResponse response = service.topEarners("GBP", 5);

        assertThat(response.currencyCode()).isEqualTo("GBP");
        assertThat(response.limit()).isEqualTo(5);
        assertThat(response.earners()).hasSize(1);
        assertThat(response.earners().get(0).employeeNumber()).isEqualTo("EMP-00042");
        assertThat(response.earners().get(0).amount()).isEqualByComparingTo("250000.00");
        assertThat(response.earners().get(0).effectiveDate()).isEqualTo(effective);
    }

    /**
     * Some driver configurations still return {@code java.sql.Date} for a
     * DATE column. The normalisation helper must accept both. This test
     * locks in that portability.
     */
    @Test
    void topEarnersAcceptsSqlDateFromAlternativeDrivers() {
        java.sql.Date effective = java.sql.Date.valueOf(LocalDate.of(2025, 6, 1));
        when(repository.topEarners(any(LocalDate.class), eq("GBP"), eq(5)))
                .thenReturn(List.<Object[]>of(new Object[]{
                        42L, "EMP-00042", "Ada", "Lovelace",
                        "Engineering", "GB",
                        new BigDecimal("250000.00"), "GBP", effective
                }));

        TopEarnersResponse response = service.topEarners("GBP", 5);

        assertThat(response.earners().get(0).effectiveDate())
                .isEqualTo(LocalDate.of(2025, 6, 1));
    }

    /**
     * A non-date value in the DATE column slot is a contract violation. The
     * service must fail loudly rather than silently produce a null date,
     * because a null date in the top-earners report would corrupt the HR
     * user's view of salary history.
     */
    @Test
    void topEarnersRejectsUnexpectedDateType() {
        when(repository.topEarners(any(LocalDate.class), eq("GBP"), eq(5)))
                .thenReturn(List.<Object[]>of(new Object[]{
                        42L, "EMP-00042", "Ada", "Lovelace",
                        "Engineering", "GB",
                        new BigDecimal("250000.00"), "GBP", "2025-06-01"
                }));

        assertThatThrownBy(() -> service.topEarners("GBP", 5))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unexpected JDBC type");
    }
}