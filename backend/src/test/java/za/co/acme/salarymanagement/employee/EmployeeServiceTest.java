package za.co.acme.salarymanagement.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.data.jpa.domain.Specification;
import za.co.acme.salarymanagement.common.DuplicateEmployeeException;
import za.co.acme.salarymanagement.common.EmployeeNotFoundException;
import za.co.acme.salarymanagement.employee.dto.EmployeeRequest;
import za.co.acme.salarymanagement.employee.dto.EmployeeResponse;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository repository;

    @InjectMocks
    private EmployeeService service;

    // --- create() ---

    @Test
    void createTrimsAndNormalisesFields() {
        when(repository.existsByEmployeeNumber("EMP-1000")).thenReturn(false);
        when(repository.existsByEmail("ada@acme.test")).thenReturn(false);
        when(repository.save(any(Employee.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EmployeeRequest request = new EmployeeRequest(
                "  EMP-1000  ",
                "  Ada  ",
                "  Lovelace  ",
                "  ada@acme.test  ",
                "gb",
                "  Engineering  ",
                EmploymentStatus.ACTIVE
        );

        EmployeeResponse response = service.create(request);

        assertThat(response.employeeNumber()).isEqualTo("EMP-1000");
        assertThat(response.firstName()).isEqualTo("Ada");
        assertThat(response.lastName()).isEqualTo("Lovelace");
        assertThat(response.email()).isEqualTo("ada@acme.test");
        assertThat(response.countryCode()).isEqualTo("GB");
        assertThat(response.department()).isEqualTo("Engineering");
        assertThat(response.employmentStatus()).isEqualTo(EmploymentStatus.ACTIVE);
    }

    @Test
    void createRejectsDuplicateEmployeeNumber() {
        when(repository.existsByEmployeeNumber("EMP-1000")).thenReturn(true);

        EmployeeRequest request = new EmployeeRequest(
                "EMP-1000", "Ada", "Lovelace", "ada@acme.test",
                "GB", "Engineering", EmploymentStatus.ACTIVE
        );

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(DuplicateEmployeeException.class)
                .hasMessageContaining("Employee number already exists");

        verify(repository, never()).save(any(Employee.class));
    }

    @Test
    void createRejectsDuplicateEmail() {
        when(repository.existsByEmployeeNumber("EMP-1000")).thenReturn(false);
        when(repository.existsByEmail("ada@acme.test")).thenReturn(true);

        EmployeeRequest request = new EmployeeRequest(
                "EMP-1000", "Ada", "Lovelace", "ada@acme.test",
                "GB", "Engineering", EmploymentStatus.ACTIVE
        );

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(DuplicateEmployeeException.class)
                .hasMessageContaining("Email already exists");

        verify(repository, never()).save(any(Employee.class));
    }

    // --- getById() ---

    @Test
    void getByIdReturnsResponseWhenPresent() {
        Employee stored = new Employee(
                "EMP-1000", "Ada", "Lovelace", "ada@acme.test",
                "GB", "Engineering", EmploymentStatus.ACTIVE
        );
        when(repository.findById(42L)).thenReturn(Optional.of(stored));

        EmployeeResponse response = service.getById(42L);

        assertThat(response.employeeNumber()).isEqualTo("EMP-1000");
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(999L))
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessageContaining("999");
    }

    // --- list(Pageable) ---

    @Test
    void listWithoutCriteriaUsesFindAll() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Employee> page = new PageImpl<>(List.of(), pageable, 0);
        when(repository.findAll(pageable)).thenReturn(page);

        Page<EmployeeResponse> result = service.list(pageable);

        assertThat(result).isEmpty();
        verify(repository).findAll(pageable);
    }

    // --- list(criteria, Pageable) ---

    @Test
    void listWithEmptyCriteriaUsesFindAll() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Employee> page = new PageImpl<>(List.of(), pageable, 0);
        when(repository.findAll(pageable)).thenReturn(page);

        EmployeeSearchCriteria empty = new EmployeeSearchCriteria(null, null, null, null);
        Page<EmployeeResponse> result = service.list(empty, pageable);

        assertThat(result).isEmpty();
        verify(repository).findAll(pageable);
    }

    @Test
    void listWithCriteriaUsesFindAllSpecification() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Employee> page = new PageImpl<>(List.of(), pageable, 0);
        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        EmployeeSearchCriteria criteria = new EmployeeSearchCriteria(
                "ada", "GB", "Engineering", EmploymentStatus.ACTIVE
        );
        Page<EmployeeResponse> result = service.list(criteria, pageable);

        assertThat(result).isEmpty();
        verify(repository).findAll(any(Specification.class), eq(pageable));
    }
}