package za.co.acme.salarymanagement.salary.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import za.co.acme.salarymanagement.salary.SalaryRecord;

/**
 * Outgoing payload for a salary record. The employee reference is exposed
 * as a plain id; callers who need employee details use the employee
 * endpoint separately. This keeps salary responses small and lets the
 * frontend load the two entities independently.
 */
public record SalaryRecordResponse(
        Long id,
        Long employeeId,
        BigDecimal amount,
        String currencyCode,
        LocalDate effectiveDate,
        OffsetDateTime createdAt
) {
    public static SalaryRecordResponse from(SalaryRecord record) {
        return new SalaryRecordResponse(
                record.getId(),
                record.getEmployee().getId(),
                record.getAmount(),
                record.getCurrencyCode(),
                record.getEffectiveDate(),
                record.getCreatedAt()
        );
    }
}