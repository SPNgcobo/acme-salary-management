package za.co.acme.salarymanagement.salary.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Incoming payload for creating a salary record. Amount must be positive,
 * currency must be a three-letter code (validated against ISO 4217 in the
 * service), and effective date is required.
 *
 * <p>The amount's decimal shape (13 integer digits, 2 fractional) matches
 * the {@code NUMERIC(15,2)} column so validation failures are caught at the
 * edge rather than at the database.
 */
public record SalaryRecordRequest(

        @NotNull
        @DecimalMin(value = "0.01", message = "amount must be greater than zero")
        @Digits(integer = 13, fraction = 2,
                message = "amount must have at most 13 integer digits and 2 decimal places")
        BigDecimal amount,

        @NotNull
        @Size(min = 3, max = 3, message = "currencyCode must be exactly 3 characters")
        String currencyCode,

        @NotNull
        LocalDate effectiveDate
) {
}