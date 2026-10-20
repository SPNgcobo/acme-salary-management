package za.co.acme.salarymanagement.employee;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.co.acme.salarymanagement.employee.dto.EmployeeRequest;
import za.co.acme.salarymanagement.employee.dto.EmployeeResponse;

/**
 * HTTP entry points for employee operations. Everything here delegates to
 * {@link EmployeeService}. Controller methods stay thin.
 *
 * <p>The {@code @Validated} annotation on the class enables method-level
 * parameter validation (e.g. {@code @Max} on the {@code size} parameter).
 * Failures are translated to 400 responses by
 * {@link za.co.acme.salarymanagement.common.GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api/employees")
@Validated
public class EmployeeController {

    /**
     * Fields clients are permitted to sort by. Anything else is rejected
     * with 400. The whitelist keeps sort expressions predictable and
     * prevents accidental exposure of internal columns.
     */
    static final Set<String> SORTABLE_FIELDS = Set.of(
            "id", "employeeNumber", "firstName", "lastName", "email",
            "countryCode", "department", "employmentStatus",
            "createdAt", "updatedAt"
    );

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request) {
        EmployeeResponse created = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public EmployeeResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    /**
     * Paginated, searchable, filterable employee list.
     *
     * <p>Search: free-text partial match across first name, last name,
     * employee number, and email (case-insensitive).
     *
     * <p>Filters: exact match on country code (ISO 3166-1 alpha-2,
     * case-insensitive), department (case-insensitive), and employment
     * status (ACTIVE / ON_LEAVE / TERMINATED). All filters combine with AND.
     *
     * <p>Pagination: {@code page} defaults to 0; {@code size} defaults to 20
     * and is capped at 100. {@code sort} must be one of
     * {@link #SORTABLE_FIELDS}, with an optional {@code ,asc} or {@code ,desc}
     * suffix; the default is {@code id,asc}.
     */
    @GetMapping
    public Page<EmployeeResponse> list(
            @RequestParam(required = false)
            @Size(max = 100, message = "search must be at most 100 characters")
            String search,

            @RequestParam(required = false)
            @Pattern(regexp = "^[A-Za-z]{2}$",
                    message = "country must be a two-letter ISO 3166-1 alpha-2 code")
            String country,

            @RequestParam(required = false)
            @Size(max = 100, message = "department must be at most 100 characters")
            String department,

            @RequestParam(required = false)
            EmploymentStatus status,

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "page must be zero or greater")
            int page,

            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size must be at least 1")
            @Max(value = MAX_PAGE_SIZE, message = "size must be at most 100")
            int size,

            @RequestParam(required = false)
            String sort
    ) {
        Pageable pageable = buildPageable(page, size, sort);

        EmployeeSearchCriteria criteria = new EmployeeSearchCriteria(
                trimToNull(search),
                trimToNull(country),
                trimToNull(department),
                status
        );

        return service.list(criteria, pageable);
    }

    private Pageable buildPageable(int page, int size, String sortParam) {
        Sort sort = parseSort(sortParam);
        return PageRequest.of(page, size, sort);
    }

    /**
     * Parses {@code "field"} or {@code "field,asc"} or {@code "field,desc"}.
     * Validates the field against the whitelist; throws
     * {@link IllegalArgumentException} (mapped to 400 by the global handler)
     * for unknown fields.
     */
    private Sort parseSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by(Sort.Order.asc("id"));
        }
        String[] parts = sortParam.split(",", 2);
        String field = parts[0].trim();
        String direction = parts.length > 1 ? parts[1].trim().toLowerCase() : "asc";

        if (!SORTABLE_FIELDS.contains(field)) {
            throw new IllegalArgumentException(
                    "Unsupported sort field: " + field + ". Allowed: " + List.copyOf(SORTABLE_FIELDS)
            );
        }
        if (!direction.equals("asc") && !direction.equals("desc")) {
            throw new IllegalArgumentException(
                    "Sort direction must be 'asc' or 'desc', got: " + direction
            );
        }
        return Sort.by(
                direction.equals("asc")
                        ? Sort.Order.asc(field)
                        : Sort.Order.desc(field)
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}