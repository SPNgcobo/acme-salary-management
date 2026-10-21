package za.co.acme.salarymanagement.salary;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
import za.co.acme.salarymanagement.salary.dto.CurrentSalaryResponse;
import za.co.acme.salarymanagement.salary.dto.SalaryRecordRequest;
import za.co.acme.salarymanagement.salary.dto.SalaryRecordResponse;

/**
 * HTTP endpoints for salary record operations. Salary records are nested
 * under the employee they belong to, matching the URI shape an HR user
 * thinks in: "the salaries of employee 42".
 */
@RestController
@RequestMapping("/api/employees/{employeeId}/salaries")
@Validated
public class SalaryRecordController {

    private final SalaryRecordService service;

    public SalaryRecordController(SalaryRecordService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<SalaryRecordResponse> record(
            @PathVariable Long employeeId,
            @Valid @RequestBody SalaryRecordRequest request
    ) {
        SalaryRecordResponse created = service.record(employeeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public Page<SalaryRecordResponse> list(
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "page must be zero or greater")
            int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size must be at least 1")
            @Max(value = SalaryRecordService.MAX_PAGE_SIZE,
                    message = "size must be at most " + SalaryRecordService.MAX_PAGE_SIZE)
            int size,
            @RequestParam(required = false) String sort
    ) {
        Pageable pageable = buildPageable(page, size, sort);
        return service.list(employeeId, pageable);
    }

    @GetMapping("/current")
    public CurrentSalaryResponse current(@PathVariable Long employeeId) {
        return service.current(employeeId);
    }

    private Pageable buildPageable(int page, int size, String sortParam) {
        Sort sort = parseSort(sortParam);
        return PageRequest.of(page, size, sort);
    }

    private Sort parseSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by(Sort.Order.desc("effectiveDate"));
        }
        String[] parts = sortParam.split(",", 2);
        String field = parts[0].trim();
        String direction = parts.length > 1 ? parts[1].trim().toLowerCase() : "asc";

        if (!SalaryRecordService.SORTABLE_FIELDS.contains(field)) {
            throw new IllegalArgumentException(
                    "Unsupported sort field: " + field
                            + ". Allowed: " + SalaryRecordService.SORTABLE_FIELDS);
        }
        if (!direction.equals("asc") && !direction.equals("desc")) {
            throw new IllegalArgumentException(
                    "Sort direction must be 'asc' or 'desc', got: " + direction);
        }
        return Sort.by(direction.equals("asc")
                ? Sort.Order.asc(field)
                : Sort.Order.desc(field));
    }
}