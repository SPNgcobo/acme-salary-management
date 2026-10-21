package za.co.acme.salarymanagement.salary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import za.co.acme.salarymanagement.common.DuplicateSalaryException;
import za.co.acme.salarymanagement.common.EmployeeNotFoundException;
import za.co.acme.salarymanagement.common.InvalidCurrencyException;
import za.co.acme.salarymanagement.employee.Employee;
import za.co.acme.salarymanagement.employee.EmployeeRepository;
import za.co.acme.salarymanagement.employee.EmploymentStatus;
import za.co.acme.salarymanagement.salary.dto.CurrentSalaryResponse;
import za.co.acme.salarymanagement.salary.dto.SalaryRecordRequest;
import za.co.acme.salarymanagement.salary.dto.SalaryRecordResponse;

@ExtendWith(MockitoExtension.class)
class SalaryRecordServiceTest {

    @Mock
    private SalaryRecordRepository salaryRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private SalaryRecordService service;

    // --- record() ---

    @Test
    void recordThrowsWhenEmployeeMissing() {
        when(employeeRepository.findById(42L)).thenReturn(Optional.empty());

        SalaryRecordRequest request = new SalaryRecordRequest(
                new BigDecimal("50000.00"),
                "GBP",
                LocalDate.of(2026, 1, 1)
        );

        assertThatThrownBy(() -> service.record(42L, request))
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessageContaining("42");

        verify(salaryRepository, never()).save(any(SalaryRecord.class));
    }

    @Test
    void recordRejectsUnknownCurrency() {
        Employee employee = sampleEmployee(42L);
        when(employeeRepository.findById(42L)).thenReturn(Optional.of(employee));

        SalaryRecordRequest request = new SalaryRecordRequest(
                new BigDecimal("50000.00"),
                "XYZ",
                LocalDate.of(2026, 1, 1)
        );

        assertThatThrownBy(() -> service.record(42L, request))
                .isInstanceOf(InvalidCurrencyException.class)
                .hasMessageContaining("XYZ");

        verify(salaryRepository, never()).save(any(SalaryRecord.class));
    }

    @Test
    void recordRejectsEffectiveDateTooFarInFuture() {
        Employee employee = sampleEmployee(42L);
        when(employeeRepository.findById(42L)).thenReturn(Optional.of(employee));

        SalaryRecordRequest request = new SalaryRecordRequest(
                new BigDecimal("50000.00"),
                "GBP",
                LocalDate.now().plusYears(20)
        );

        assertThatThrownBy(() -> service.record(42L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("effectiveDate");

        verify(salaryRepository, never()).save(any(SalaryRecord.class));
    }

    @Test
    void recordRejectsDuplicateEffectiveDate() {
        Employee employee = sampleEmployee(42L);
        LocalDate date = LocalDate.of(2026, 1, 1);
        when(employeeRepository.findById(42L)).thenReturn(Optional.of(employee));
        when(salaryRepository.existsByEmployeeIdAndEffectiveDate(42L, date)).thenReturn(true);

        SalaryRecordRequest request = new SalaryRecordRequest(
                new BigDecimal("50000.00"), "GBP", date
        );

        assertThatThrownBy(() -> service.record(42L, request))
                .isInstanceOf(DuplicateSalaryException.class)
                .hasMessageContaining("2026-01-01");

        verify(salaryRepository, never()).save(any(SalaryRecord.class));
    }

    @Test
    void recordNormalisesCurrencyToUppercaseAndSaves() {
        Employee employee = sampleEmployee(42L);
        LocalDate date = LocalDate.of(2026, 1, 1);
        when(employeeRepository.findById(42L)).thenReturn(Optional.of(employee));
        when(salaryRepository.existsByEmployeeIdAndEffectiveDate(42L, date)).thenReturn(false);
        when(salaryRepository.save(any(SalaryRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SalaryRecordRequest request = new SalaryRecordRequest(
                new BigDecimal("50000.00"), "gbp", date
        );

        SalaryRecordResponse response = service.record(42L, request);

        assertThat(response.currencyCode()).isEqualTo("GBP");
        assertThat(response.amount()).isEqualByComparingTo("50000.00");
        assertThat(response.effectiveDate()).isEqualTo(date);
        verify(salaryRepository).save(any(SalaryRecord.class));
    }

    // --- current() ---

    @Test
    void currentThrowsWhenEmployeeMissing() {
        when(employeeRepository.existsById(42L)).thenReturn(false);

        assertThatThrownBy(() -> service.current(42L))
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessageContaining("42");
    }

    @Test
    void currentThrowsWhenNoEffectiveRecord() {
        when(employeeRepository.existsById(42L)).thenReturn(true);
        when(salaryRepository
                .findTopByEmployeeIdAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                        eq(42L), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.current(42L))
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessageContaining("42");
    }

    @Test
    void currentReturnsLatestEffectiveRecord() {
        Employee employee = sampleEmployee(42L);
        SalaryRecord record = new SalaryRecord(
                employee, new BigDecimal("55000.00"), "GBP",
                LocalDate.now().minusMonths(2)
        );
        when(employeeRepository.existsById(42L)).thenReturn(true);
        when(salaryRepository
                .findTopByEmployeeIdAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                        eq(42L), any(LocalDate.class)))
                .thenReturn(Optional.of(record));

        CurrentSalaryResponse response = service.current(42L);

        assertThat(response.amount()).isEqualByComparingTo("55000.00");
        assertThat(response.currencyCode()).isEqualTo("GBP");
        assertThat(response.asOfDate()).isEqualTo(LocalDate.now());
    }

    // --- list() ---

    @Test
    void listThrowsWhenEmployeeMissing() {
        when(employeeRepository.existsById(42L)).thenReturn(false);

        assertThatThrownBy(() -> service.list(42L, PageRequest.of(0, 20)))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void listReturnsMappedPage() {
        Employee employee = sampleEmployee(42L);
        SalaryRecord record = new SalaryRecord(
                employee, new BigDecimal("50000.00"), "GBP",
                LocalDate.of(2025, 1, 1)
        );
        Page<SalaryRecord> page = new PageImpl<>(
                List.of(record), PageRequest.of(0, 20), 1
        );
        when(employeeRepository.existsById(42L)).thenReturn(true);
        when(salaryRepository.findByEmployeeId(eq(42L), any(Pageable.class)))
                .thenReturn(page);

        Page<SalaryRecordResponse> result = service.list(42L, PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).currencyCode()).isEqualTo("GBP");
    }

    @Test
    void listRejectsUnsupportedSortField() {
        when(employeeRepository.existsById(42L)).thenReturn(true);

        Pageable badSort = PageRequest.of(0, 20, Sort.by("hackedField"));

        assertThatThrownBy(() -> service.list(42L, badSort))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hackedField");
    }

    @Test
    void listCapsPageSizeAtMaximum() {
        Employee employee = sampleEmployee(42L);
        when(employeeRepository.existsById(42L)).thenReturn(true);
        when(salaryRepository.findByEmployeeId(eq(42L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.list(42L, PageRequest.of(0, 9999));

        // The service should have applied the cap; verify by re-inspecting
        // the pageable passed to the repository.
        verify(salaryRepository).findByEmployeeId(eq(42L),
                org.mockito.ArgumentMatchers.argThat(p ->
                        p.getPageSize() <= SalaryRecordService.MAX_PAGE_SIZE));
    }

    private Employee sampleEmployee(Long id) {
        Employee e = new Employee(
                "EMP-" + id, "Ada", "Lovelace",
                "ada" + id + "@test.acme", "GB", "Engineering",
                EmploymentStatus.ACTIVE
        );
        // Reflect the id onto the entity so getId() works. We can't call a setter
        // (there is none), so we rely on the mock's answer for the id being
        // non-null in tests that need it. Where a real id matters, the entity
        // is created via the persistence layer in repository tests instead.
        return e;
    }
}