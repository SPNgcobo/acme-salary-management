package za.co.acme.salarymanagement.salary;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import tools.jackson.databind.ObjectMapper;
import za.co.acme.salarymanagement.common.DuplicateSalaryException;
import za.co.acme.salarymanagement.common.EmployeeNotFoundException;
import za.co.acme.salarymanagement.common.GlobalExceptionHandler;
import za.co.acme.salarymanagement.common.InvalidCurrencyException;
import za.co.acme.salarymanagement.salary.dto.CurrentSalaryResponse;
import za.co.acme.salarymanagement.salary.dto.SalaryRecordRequest;
import za.co.acme.salarymanagement.salary.dto.SalaryRecordResponse;

@WebMvcTest(controllers = SalaryRecordController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, MethodValidationPostProcessor.class})
class SalaryRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SalaryRecordService service;

    // --- record ---

    @Test
    void recordReturns201WithBody() throws Exception {
        SalaryRecordResponse response = new SalaryRecordResponse(
                100L, 42L, new BigDecimal("50000.00"), "GBP",
                LocalDate.of(2026, 1, 1), OffsetDateTime.now()
        );
        when(service.record(eq(42L), any(SalaryRecordRequest.class))).thenReturn(response);

        String body = objectMapper.writeValueAsString(new SalaryRecordRequest(
                new BigDecimal("50000.00"), "GBP", LocalDate.of(2026, 1, 1)
        ));

        mockMvc.perform(post("/api/employees/42/salaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.employeeId").value(42))
                .andExpect(jsonPath("$.currencyCode").value("GBP"))
                .andExpect(jsonPath("$.amount").value(50000.00));
    }

    @Test
    void recordReturns400OnInvalidCurrency() throws Exception {
        when(service.record(eq(42L), any(SalaryRecordRequest.class)))
                .thenThrow(new InvalidCurrencyException("XYZ"));

        String body = objectMapper.writeValueAsString(new SalaryRecordRequest(
                new BigDecimal("50000.00"), "XYZ", LocalDate.of(2026, 1, 1)
        ));

        mockMvc.perform(post("/api/employees/42/salaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown ISO 4217 currency code: XYZ"));
    }

    @Test
    void recordReturns400OnNegativeAmount() throws Exception {
        String body = objectMapper.writeValueAsString(new SalaryRecordRequest(
                new BigDecimal("-1.00"), "GBP", LocalDate.of(2026, 1, 1)
        ));

        mockMvc.perform(post("/api/employees/42/salaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recordReturns409OnDuplicateEffectiveDate() throws Exception {
        LocalDate date = LocalDate.of(2026, 1, 1);
        when(service.record(eq(42L), any(SalaryRecordRequest.class)))
                .thenThrow(new DuplicateSalaryException(42L, date));

        String body = objectMapper.writeValueAsString(new SalaryRecordRequest(
                new BigDecimal("50000.00"), "GBP", date
        ));

        mockMvc.perform(post("/api/employees/42/salaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "A salary record already exists for employee 42 effective 2026-01-01"));
    }

    @Test
    void recordReturns404WhenEmployeeMissing() throws Exception {
        when(service.record(eq(42L), any(SalaryRecordRequest.class)))
                .thenThrow(new EmployeeNotFoundException(42L));

        String body = objectMapper.writeValueAsString(new SalaryRecordRequest(
                new BigDecimal("50000.00"), "GBP", LocalDate.of(2026, 1, 1)
        ));

        mockMvc.perform(post("/api/employees/42/salaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    // --- current ---

    @Test
    void currentReturnsCurrentSalary() throws Exception {
        when(service.current(42L)).thenReturn(new CurrentSalaryResponse(
                42L, new BigDecimal("55000.00"), "GBP",
                LocalDate.of(2025, 1, 1), LocalDate.now()
        ));

        mockMvc.perform(get("/api/employees/42/salaries/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(42))
                .andExpect(jsonPath("$.currencyCode").value("GBP"))
                .andExpect(jsonPath("$.amount").value(55000.00));
    }

    @Test
    void currentReturns404WhenNoEffectiveSalary() throws Exception {
        when(service.current(42L)).thenThrow(new EmployeeNotFoundException(42L));

        mockMvc.perform(get("/api/employees/42/salaries/current"))
                .andExpect(status().isNotFound());
    }

    // --- list ---

    @Test
    void listReturnsPage() throws Exception {
        SalaryRecordResponse record = new SalaryRecordResponse(
                100L, 42L, new BigDecimal("50000.00"), "GBP",
                LocalDate.of(2026, 1, 1), OffsetDateTime.now()
        );
        Page<SalaryRecordResponse> page = new PageImpl<>(
                List.of(record), PageRequest.of(0, 20), 1
        );
        when(service.list(eq(42L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/employees/42/salaries")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].currencyCode").value("GBP"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listReturns400WhenSizeExceedsCap() throws Exception {
        mockMvc.perform(get("/api/employees/42/salaries").param("size", "9999"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listReturns400WhenSortFieldIsNotWhitelisted() throws Exception {
        mockMvc.perform(get("/api/employees/42/salaries").param("sort", "hackedField,asc"))
                .andExpect(status().isBadRequest());
    }
}