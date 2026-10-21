package za.co.acme.salarymanagement.common;

/**
 * Thrown when a salary record references a currency code that is not a
 * valid ISO 4217 code. Mapped to HTTP 400 by
 * {@link GlobalExceptionHandler}.
 */
public class InvalidCurrencyException extends RuntimeException {

    public InvalidCurrencyException(String code) {
        super("Unknown ISO 4217 currency code: " + code);
    }
}