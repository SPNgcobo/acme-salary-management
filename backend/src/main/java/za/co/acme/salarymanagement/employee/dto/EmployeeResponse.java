package za.co.acme.salarymanagement.employee.dto;

import java.time.OffsetDateTime;
import za.co.acme.salarymanagement.employee.Employee;
import za.co.acme.salarymanagement.employee.EmploymentStatus;

/**
 * Outgoing payload for an employee. Kept deliberately separate from the
 * entity so database structure and API contract can evolve independently.
 */
public record EmployeeResponse(
        Long id,
        String employeeNumber,
        String firstName,
        String lastName,
        String email,
        String countryCode,
        String department,
        EmploymentStatus employmentStatus,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getEmployeeNumber(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getCountryCode(),
                employee.getDepartment(),
                employee.getEmploymentStatus(),
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }
}