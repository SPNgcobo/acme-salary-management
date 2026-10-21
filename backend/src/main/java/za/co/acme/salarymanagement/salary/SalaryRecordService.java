package za.co.acme.salarymanagement.salary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.acme.salarymanagement.common.DuplicateSalaryException;
import za.co.acme.salarymanagement.common.EmployeeNotFoundException;
import za.co.acme.salarymanagement.common.InvalidCurrencyException;
import za.co.acme.salarymanagement.employee.Employee;
import za.co.acme.salarymanagement.employee.EmployeeRepository;
import za.co.acme.salarymanagement.salary.dto.CurrentSalaryResponse;
import za.co.acme.salarymanagement.salary.dto.SalaryRecordRequest;
import za.co.acme.salarymanagement.salary.dto.SalaryRecordResponse;

/**
 * Business rules for salary records.
 *
 * <p>Salary history is append-only. The service never updates or deletes
 * existing records; a "salary change" always inserts a new row with a new
 * effective date.
 *
 * <p>Current-salary resolution is "latest effective_date on or before
 * today". Future-dated records appear in history but are not current until
 * their effective date arrives.
 */
@Service
public class SalaryRecordService {

    static final Set<String> SORTABLE_FIELDS = Set.of(
            "effectiveDate", "amount", "createdAt"
    );

    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 100;

    /** Upper bound on how far in the future an effective date may be. */
    static final int MAX_FUTURE_YEARS = 10;

    private final SalaryRecordRepository salaryRepository;
    private final EmployeeRepository employeeRepository;

    public SalaryRecordService(
            SalaryRecordRepository salaryRepository,
            EmployeeRepository employeeRepository
    ) {
        this.salaryRepository = salaryRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    public SalaryRecordResponse record(Long employeeId, SalaryRecordRequest request) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException(employeeId));

        BigDecimal amount = request.amount();
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }

        LocalDate effectiveDate = request.effectiveDate();
        if (effectiveDate == null) {
            throw new IllegalArgumentException("effectiveDate is required");
        }
        LocalDate maxFuture = LocalDate.now().plusYears(MAX_FUTURE_YEARS);
        if (effectiveDate.isAfter(maxFuture)) {
            throw new IllegalArgumentException(
                    "effectiveDate must be on or before " + maxFuture);
        }

        String currency = normaliseAndValidateCurrency(request.currencyCode());

        if (salaryRepository.existsByEmployeeIdAndEffectiveDate(employeeId, effectiveDate)) {
            throw new DuplicateSalaryException(employeeId, effectiveDate);
        }

        SalaryRecord saved = salaryRepository.save(
                new SalaryRecord(employee, amount, currency, effectiveDate)
        );
        return SalaryRecordResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<SalaryRecordResponse> list(Long employeeId, Pageable pageable) {
        requireEmployee(employeeId);
        Pageable safePageable = enforceWhitelistAndDefaults(pageable);
        return salaryRepository.findByEmployeeId(employeeId, safePageable)
                .map(SalaryRecordResponse::from);
    }

    @Transactional(readOnly = true)
    public CurrentSalaryResponse current(Long employeeId) {
        requireEmployee(employeeId);
        LocalDate today = LocalDate.now();
        return salaryRepository
                .findTopByEmployeeIdAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(employeeId, today)
                .map(record -> CurrentSalaryResponse.from(record, today))
                .orElseThrow(() -> new EmployeeNotFoundException(employeeId));
    }

    private void requireEmployee(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new EmployeeNotFoundException(employeeId);
        }
    }

    private String normaliseAndValidateCurrency(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new IllegalArgumentException("currencyCode is required");
        }
        String code = rawCode.trim().toUpperCase();
        try {
            Currency.getInstance(code);
        } catch (IllegalArgumentException e) {
            throw new InvalidCurrencyException(code);
        }
        return code;
    }

    /**
     * Applies the sort-field whitelist and page-size cap. The controller
     * could do this, but doing it in the service means callers who bypass
     * the controller (e.g. tests, future internal callers) also get the
     * safety net.
     */
    private Pageable enforceWhitelistAndDefaults(Pageable pageable) {
        int size = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        if (size < 1) {
            size = DEFAULT_PAGE_SIZE;
        }

        Sort sort = pageable.getSort();
        if (sort.isUnsorted()) {
            sort = Sort.by(Sort.Order.desc("effectiveDate"));
        } else {
            for (Sort.Order order : sort) {
                if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                    throw new IllegalArgumentException(
                            "Unsupported sort field: " + order.getProperty()
                                    + ". Allowed: " + List.copyOf(SORTABLE_FIELDS));
                }
            }
        }

        return PageRequest.of(pageable.getPageNumber(), size, sort);
    }
}