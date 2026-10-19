package za.co.acme.salarymanagement.employee;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.acme.salarymanagement.common.EmployeeNotFoundException;
import za.co.acme.salarymanagement.employee.dto.EmployeeRequest;
import za.co.acme.salarymanagement.employee.dto.EmployeeResponse;

/**
 * Business rules for employees. No HTTP concepts here — controllers
 * translate HTTP in and out, this service owns the domain behaviour.
 */
@Service
public class EmployeeService {

    private final EmployeeRepository repository;

    public EmployeeService(EmployeeRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        // Trim string inputs defensively, then check uniqueness at the service
        // layer so users get a friendly message. The database unique constraints
        // are still the ultimate authority.
        String employeeNumber = request.employeeNumber().trim();
        String email = request.email().trim();

        if (repository.existsByEmployeeNumber(employeeNumber)) {
            throw new IllegalArgumentException("Employee number already exists: " + employeeNumber);
        }
        if (repository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }

        Employee employee = new Employee(
                employeeNumber,
                request.firstName().trim(),
                request.lastName().trim(),
                email,
                request.countryCode().trim().toUpperCase(),
                request.department().trim(),
                request.employmentStatus()
        );

        return EmployeeResponse.from(repository.save(employee));
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getById(Long id) {
        return repository.findById(id)
                .map(EmployeeResponse::from)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> list(Pageable pageable) {
        return repository.findAll(pageable).map(EmployeeResponse::from);
    }
}