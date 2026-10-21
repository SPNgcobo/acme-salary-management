package za.co.acme.salarymanagement.report;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import za.co.acme.salarymanagement.common.GlobalExceptionHandler;
import za.co.acme.salarymanagement.common.InvalidCurrencyException;
import za.co.acme.salarymanagement.report.dto.CurrencyTotal;
import za.co.acme.salarymanagement.report.dto.DashboardResponse;
import za.co.acme.salarymanagement.report.dto.DistributionBucket;
import za.co.acme.salarymanagement.report.dto.DistributionResponse;
import za.co.acme.salarymanagement.report.dto.SalaryAggregateResponse;
import za.co.acme.salarymanagement.report.dto.SalaryAggregateRow;
import za.co.acme.salarymanagement.report.dto.TopEarnerRow;
import za.co.acme.salarymanagement.report.dto.TopEarnersResponse;

@WebMvcTest(controllers = ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, MethodValidationPostProcessor.class})
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService service;

    // --- dashboard ---

    @Test
    void dashboardReturnsAggregates() throws Exception {
        when(service.dashboard()).thenReturn(new DashboardResponse(
                10000L, 9500L, 12L, 8L,
                List.of(new CurrencyTotal("GBP", new BigDecimal("250000000.00"), 3000L,
                        new BigDecimal("83333.33"))),
                OffsetDateTime.now()
        ));

        mockMvc.perform(get("/api/reports/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEmployees").value(10000))
                .andExpect(jsonPath("$.employeesWithCurrentSalary").value(9500))
                .andExpect(jsonPath("$.countriesRepresented").value(12))
                .andExpect(jsonPath("$.departmentsRepresented").value(8))
                .andExpect(jsonPath("$.salaryTotalsByCurrency").isArray())
                .andExpect(jsonPath("$.salaryTotalsByCurrency[0].currencyCode").value("GBP"));
    }

    // --- salary-by-country ---

    @Test
    void salaryByCountryReturnsRows() throws Exception {
        when(service.salaryByCountry(eq("GBP"))).thenReturn(new SalaryAggregateResponse(
                "country", "GBP",
                List.of(new SalaryAggregateRow("GB", 10L,
                        new BigDecimal("30000.00"), new BigDecimal("90000.00"),
                        new BigDecimal("60000.00"))),
                OffsetDateTime.now()
        ));

        mockMvc.perform(get("/api/reports/salary-by-country").param("currency", "GBP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currencyCode").value("GBP"))
                .andExpect(jsonPath("$.groupingDimension").value("country"))
                .andExpect(jsonPath("$.rows").isArray())
                .andExpect(jsonPath("$.rows[0].groupKey").value("GB"));
    }

    @Test
    void salaryByCountryRejectsUnknownCurrency() throws Exception {
        when(service.salaryByCountry(anyString()))
                .thenThrow(new InvalidCurrencyException("XYZ"));

        mockMvc.perform(get("/api/reports/salary-by-country").param("currency", "XYZ"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown ISO 4217 currency code: XYZ"));
    }

    @Test
    void salaryByCountryRejectsMissingCurrency() throws Exception {
        mockMvc.perform(get("/api/reports/salary-by-country"))
                .andExpect(status().isBadRequest());
    }

    // --- salary-by-department ---

    @Test
    void salaryByDepartmentReturnsRows() throws Exception {
        when(service.salaryByDepartment(eq("USD"))).thenReturn(new SalaryAggregateResponse(
                "department", "USD",
                List.of(new SalaryAggregateRow("Engineering", 20L,
                        new BigDecimal("50000.00"), new BigDecimal("200000.00"),
                        new BigDecimal("100000.00"))),
                OffsetDateTime.now()
        ));

        mockMvc.perform(get("/api/reports/salary-by-department").param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupingDimension").value("department"))
                .andExpect(jsonPath("$.rows[0].groupKey").value("Engineering"));
    }

    @Test
    void salaryByDepartmentRejectsUnknownCurrency() throws Exception {
        when(service.salaryByDepartment(anyString()))
                .thenThrow(new InvalidCurrencyException("ZZZ"));

        mockMvc.perform(get("/api/reports/salary-by-department").param("currency", "ZZZ"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void salaryByCountryReturnsConsistentErrorBodyForMissingCurrency() throws Exception {
        mockMvc.perform(get("/api/reports/salary-by-country"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Required parameter 'currency' is missing"))
                .andExpect(jsonPath("$.details[0].field").value("currency"));
    }

    // --- salary-distribution ---

    @Test
    void salaryDistributionReturnsBuckets() throws Exception {
        when(service.salaryDistribution(eq("GBP"), any(BigDecimal.class)))
                .thenReturn(new DistributionResponse(
                        "GBP", new BigDecimal("10000.00"), 10L,
                        List.of(new DistributionBucket(
                                new BigDecimal("40000.00"),
                                new BigDecimal("50000.00"),
                                3L)),
                        OffsetDateTime.now()
                ));

        mockMvc.perform(get("/api/reports/salary-distribution")
                        .param("currency", "GBP")
                        .param("bucketSize", "10000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currencyCode").value("GBP"))
                .andExpect(jsonPath("$.totalEmployeesInCurrency").value(10))
                .andExpect(jsonPath("$.buckets[0].employeeCount").value(3));
    }

    @Test
    void salaryDistributionRejectsMissingBucketSize() throws Exception {
        mockMvc.perform(get("/api/reports/salary-distribution")
                        .param("currency", "GBP"))
                .andExpect(status().isBadRequest());
    }

    // --- top-earners ---

    @Test
    void topEarnersReturnsEarners() throws Exception {
        when(service.topEarners(eq("GBP"), eq(5))).thenReturn(new TopEarnersResponse(
                "GBP", 5,
                List.of(new TopEarnerRow(42L, "EMP-00042", "Ada", "Lovelace",
                        "Engineering", "GB", new BigDecimal("250000.00"),
                        "GBP", LocalDate.of(2025, 6, 1))),
                OffsetDateTime.now()
        ));

        mockMvc.perform(get("/api/reports/top-earners")
                        .param("currency", "GBP")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currencyCode").value("GBP"))
                .andExpect(jsonPath("$.limit").value(5))
                .andExpect(jsonPath("$.earners[0].employeeNumber").value("EMP-00042"));
    }

    @Test
    void topEarnersRejectsLimitAboveMax() throws Exception {
        mockMvc.perform(get("/api/reports/top-earners")
                        .param("currency", "GBP")
                        .param("limit", "500"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void topEarnersUsesDefaultLimitWhenOmitted() throws Exception {
        when(service.topEarners(eq("GBP"), anyInt())).thenReturn(new TopEarnersResponse(
                "GBP", 10, List.of(), OffsetDateTime.now()
        ));

        mockMvc.perform(get("/api/reports/top-earners").param("currency", "GBP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limit").value(10));
    }
}