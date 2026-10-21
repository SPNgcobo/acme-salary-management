package za.co.acme.salarymanagement.common;

import java.time.LocalDate;

/**
 * Thrown when a salary record already exists for the same employee and
 * effective date. Mapped to HTTP 409 by
 * {@link GlobalExceptionHandler}.
 */
public class DuplicateSalaryException extends RuntimeException {

    public DuplicateSalaryException(Long employeeId, LocalDate effectiveDate) {
        super("A salary record already exists for employee "
                + employeeId + " effective " + effectiveDate);
    }
}