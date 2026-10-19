package za.co.acme.salarymanagement.common;

/**
 * Thrown when a requested employee does not exist. Mapped to HTTP 404 by
 * {@link GlobalExceptionHandler}.
 */
public class EmployeeNotFoundException extends RuntimeException {

    public EmployeeNotFoundException(Long id) {
        super("Employee not found with id: " + id);
    }
}