package za.co.acme.salarymanagement.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.acme.salarymanagement.common.EmployeeNotFoundException;
import za.co.acme.salarymanagement.employee.dto.EmployeeRequest;
import za.co.acme.salarymanagement.employee.dto.EmployeeResponse;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository repository;

    @InjectMocks
    private EmployeeService service;

    @Test
    void createTrimsAndNormalisesFields() {
        when(repository.existsByEmployeeNumber("EMP-1000")).thenReturn(false);
        when(repository.existsByEmail("ada@acme.test")).thenReturn(false);
        when(repository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

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
                .isInstanceOf(IllegalArgumentException.class)
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
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already exists");

        verify(repository, never()).save(any(Employee.class));
    }

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
}