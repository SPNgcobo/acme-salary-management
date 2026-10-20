package za.co.acme.salarymanagement.employee;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.acme.salarymanagement.common.DuplicateEmployeeException;
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
        String employeeNumber = request.employeeNumber().trim();
        String email = request.email().trim();

        if (repository.existsByEmployeeNumber(employeeNumber)) {
            throw new DuplicateEmployeeException("Employee number already exists: " + employeeNumber);
        }
        if (repository.existsByEmail(email)) {
            throw new DuplicateEmployeeException("Email already exists: " + email);
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

    /**
     * Unfiltered list. Retained for callers (e.g. tests) that do not need
     * search or filter behaviour.
     */
    @Transactional(readOnly = true)
    public Page<EmployeeResponse> list(Pageable pageable) {
        return repository.findAll(pageable).map(EmployeeResponse::from);
    }

    /**
     * Filtered list. If the criteria contain no active filter, falls back to
     * the unfiltered list to avoid passing a null specification to the
     * repository (which would throw).
     */
    @Transactional(readOnly = true)
    public Page<EmployeeResponse> list(EmployeeSearchCriteria criteria, Pageable pageable) {
        Specification<Employee> spec = EmployeeSpecifications.fromCriteria(criteria);
        if (spec == null) {
            return repository.findAll(pageable).map(EmployeeResponse::from);
        }
        return repository.findAll(spec, pageable).map(EmployeeResponse::from);
    }
}