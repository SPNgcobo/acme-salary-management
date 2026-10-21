package za.co.acme.salarymanagement.salary.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import za.co.acme.salarymanagement.salary.SalaryRecord;

/**
 * Outgoing payload for an employee's current salary as of today. Includes
 * {@code asOfDate} to make the "current as of" semantics explicit in the
 * response, which is important when the caller runs the query at some
 * point and reads the response later.
 */
public record CurrentSalaryResponse(
        Long employeeId,
        BigDecimal amount,
        String currencyCode,
        LocalDate effectiveDate,
        LocalDate asOfDate
) {
    public static CurrentSalaryResponse from(SalaryRecord record, LocalDate asOfDate) {
        return new CurrentSalaryResponse(
                record.getEmployee().getId(),
                record.getAmount(),
                record.getCurrencyCode(),
                record.getEffectiveDate(),
                asOfDate
        );
    }
}