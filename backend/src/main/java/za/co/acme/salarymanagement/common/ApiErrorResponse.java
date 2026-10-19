package za.co.acme.salarymanagement.common;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Consistent error body returned for any failed API request. Field-level
 * validation errors appear in {@link #details()}.
 */
public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> details
) {
    public record FieldError(String field, String message) {
    }
}