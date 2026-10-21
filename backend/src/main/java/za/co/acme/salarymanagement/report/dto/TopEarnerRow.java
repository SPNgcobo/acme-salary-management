package za.co.acme.salarymanagement.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One row of the top-earners report. All amounts are in the single
 * currency selected by the caller.
 *
 * <p>{@code employeeNumber} is included so an HR user can find the same
 * employee via the directory without needing to know the internal id. The
 * id is included too because the frontend may want to link directly to
 * the employee detail page.
 */
public record TopEarnerRow(
        Long employeeId,
        String employeeNumber,
        String firstName,
        String lastName,
        String department,
        String countryCode,
        BigDecimal amount,
        String currencyCode,
        LocalDate effectiveDate
) {
}