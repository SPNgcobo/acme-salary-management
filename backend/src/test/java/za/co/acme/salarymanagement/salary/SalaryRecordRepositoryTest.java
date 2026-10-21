package za.co.acme.salarymanagement.salary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import za.co.acme.salarymanagement.employee.Employee;
import za.co.acme.salarymanagement.employee.EmployeeRepository;
import za.co.acme.salarymanagement.employee.EmploymentStatus;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
class SalaryRecordRepositoryTest {

    @Autowired
    private SalaryRecordRepository salaryRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    void savesAndFindsByEmployeeId() {
        Employee employee = persistEmployee();
        salaryRepository.save(new SalaryRecord(
                employee, new BigDecimal("50000.00"), "GBP", LocalDate.of(2025, 1, 1)));
        salaryRepository.save(new SalaryRecord(
                employee, new BigDecimal("55000.00"), "GBP", LocalDate.of(2026, 1, 1)));

        // Scope to the employee this test created. The shared test database may
        // contain salary rows from Postman runs or from earlier test runs; those
        // belong to different employees and must not be counted here.
        List<SalaryRecord> mine = salaryRepository.findAll().stream()
                .filter(r -> r.getEmployee().getId().equals(employee.getId()))
                .toList();

        assertThat(mine).hasSize(2);
    }

    @Test
    void rejectsDuplicateEffectiveDateForSameEmployee() {
        Employee employee = persistEmployee();
        LocalDate date = LocalDate.of(2025, 6, 1);
        salaryRepository.save(new SalaryRecord(
                employee, new BigDecimal("50000.00"), "GBP", date));
        salaryRepository.flush();

        assertThatThrownBy(() -> salaryRepository.saveAndFlush(new SalaryRecord(
                employee, new BigDecimal("60000.00"), "GBP", date)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsNonPositiveAmount() {
        Employee employee = persistEmployee();

        assertThatThrownBy(() -> salaryRepository.saveAndFlush(new SalaryRecord(
                employee, new BigDecimal("-1.00"), "GBP", LocalDate.of(2025, 1, 1))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void currentResolvesLatestEffectiveDateOnOrBeforeToday() {
        Employee employee = persistEmployee();
        LocalDate today = LocalDate.now();
        salaryRepository.save(new SalaryRecord(
                employee, new BigDecimal("40000.00"), "GBP", today.minusYears(2)));
        salaryRepository.save(new SalaryRecord(
                employee, new BigDecimal("50000.00"), "GBP", today.minusYears(1)));
        salaryRepository.save(new SalaryRecord(
                employee, new BigDecimal("60000.00"), "GBP", today.plusDays(30)));
        salaryRepository.flush();

        Optional<SalaryRecord> current = salaryRepository
                .findTopByEmployeeIdAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                        employee.getId(), today);

        assertThat(current).isPresent();
        assertThat(current.get().getAmount()).isEqualByComparingTo("50000.00");
        assertThat(current.get().getEffectiveDate()).isEqualTo(today.minusYears(1));
    }

    @Test
    void currentReturnsEmptyWhenOnlyFutureDatedRecords() {
        Employee employee = persistEmployee();
        salaryRepository.save(new SalaryRecord(
                employee, new BigDecimal("60000.00"), "GBP",
                LocalDate.now().plusMonths(1)));
        salaryRepository.flush();

        Optional<SalaryRecord> current = salaryRepository
                .findTopByEmployeeIdAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                        employee.getId(), LocalDate.now());

        assertThat(current).isEmpty();
    }

    @Test
    void findByEmployeeIdSortsByEffectiveDateDescending() {
        Employee employee = persistEmployee();
        salaryRepository.save(new SalaryRecord(
                employee, new BigDecimal("40000.00"), "GBP", LocalDate.of(2024, 1, 1)));
        salaryRepository.save(new SalaryRecord(
                employee, new BigDecimal("50000.00"), "GBP", LocalDate.of(2025, 1, 1)));
        salaryRepository.save(new SalaryRecord(
                employee, new BigDecimal("60000.00"), "GBP", LocalDate.of(2026, 1, 1)));
        salaryRepository.flush();

        Page<SalaryRecord> page = salaryRepository.findByEmployeeId(
                employee.getId(),
                PageRequest.of(0, 10, Sort.by(Sort.Order.desc("effectiveDate")))
        );

        assertThat(page.getContent())
                .extracting(SalaryRecord::getEffectiveDate)
                .containsExactly(
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2025, 1, 1),
                        LocalDate.of(2024, 1, 1)
                );
    }

    private Employee persistEmployee() {
        String number = "T-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return employeeRepository.save(new Employee(
                number, "Test", "Employee",
                number.toLowerCase() + "@test.acme",
                "GB", "Engineering", EmploymentStatus.ACTIVE
        ));
    }
}