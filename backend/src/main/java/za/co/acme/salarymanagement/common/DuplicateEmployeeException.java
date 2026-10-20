package za.co.acme.salarymanagement.common;

/**
 * Thrown when a create request would produce a duplicate employee number or
 * email. Mapped to HTTP 409 Conflict by
 * {@link GlobalExceptionHandler}.
 */
public class DuplicateEmployeeException extends RuntimeException {

    public DuplicateEmployeeException(String message) {
        super(message);
    }
}