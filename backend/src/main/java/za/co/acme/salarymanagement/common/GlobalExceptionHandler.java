package za.co.acme.salarymanagement.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Translates exceptions thrown by controllers into a single, consistent
 * JSON error shape. Every error the client sees is this structure.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<ApiErrorResponse.FieldError> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new ApiErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();

        return buildBadRequest(request, "Validation failed", details);
    }

    /**
     * Handles failures on method-level parameters, e.g.
     * {@code @RequestParam @Max(100) int size}. Those raise a
     * {@link ConstraintViolationException} rather than
     * {@link MethodArgumentNotValidException}.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        List<ApiErrorResponse.FieldError> details = ex.getConstraintViolations()
                .stream()
                .map(this::toFieldError)
                .toList();
        return buildBadRequest(request, "Invalid request parameter", details);
    }

    /**
     * Handles type conversion failures on method parameters, e.g. an unknown
     * enum value in {@code status=BANANA} or a non-numeric {@code page}.
     * Spring wraps the underlying IllegalArgumentException in
     * {@link MethodArgumentTypeMismatchException}; this handler returns a
     * clean 400 rather than letting it fall through to a generic mapping.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        String field = ex.getName();
        String required = ex.getRequiredType() != null
                ? ex.getRequiredType().getSimpleName()
                : "expected type";
        String message = "Invalid value for parameter '" + field
                + "': expected " + required;
        return buildBadRequest(request,
                "Invalid request parameter",
                List.of(new ApiErrorResponse.FieldError(field, message)));
    }

    /**
     * Generic 400 for illegal arguments that are the caller's fault (e.g.
     * unsupported sort field). Domain-level conflicts (duplicate employee
     * number) are raised as a separate exception and mapped to 409.
     *
     * <p>If you introduce a dedicated exception type for client-input
     * violations in the future, replace this handler with one for that
     * specific type and remove this one.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        return buildBadRequest(request, ex.getMessage(), List.of());
    }

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            EmployeeNotFoundException ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse body = new ApiErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                request.getRequestURI(),
                List.of()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(DuplicateEmployeeException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicate(
            DuplicateEmployeeException ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse body = new ApiErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                request.getRequestURI(),
                List.of()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    private ResponseEntity<ApiErrorResponse> buildBadRequest(
            HttpServletRequest request,
            String message,
            List<ApiErrorResponse.FieldError> details
    ) {
        ApiErrorResponse body = new ApiErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                message,
                request.getRequestURI(),
                details
        );
        return ResponseEntity.badRequest().body(body);
    }

    private ApiErrorResponse.FieldError toFieldError(ConstraintViolation<?> v) {
        String path = v.getPropertyPath().toString();
        int lastDot = path.lastIndexOf('.');
        String field = lastDot >= 0 ? path.substring(lastDot + 1) : path;
        return new ApiErrorResponse.FieldError(field, v.getMessage());
    }
}