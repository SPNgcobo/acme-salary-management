package za.co.acme.salarymanagement.salary;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import za.co.acme.salarymanagement.employee.Employee;

/**
 * Append-only salary record. Creating a new salary for an employee means
 * inserting a new row, never updating an existing one.
 *
 * <p>{@code amount} is a {@link BigDecimal} because salary values are money
 * and floating-point representation would silently lose precision. The
 * database stores it as {@code NUMERIC(15,2)}.
 *
 * <p>{@code effectiveDate} is a {@link LocalDate} because a salary change
 * takes effect on a calendar day, not at a specific instant.
 */
@Entity
@Table(name = "salary_records")
public class SalaryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected SalaryRecord() {
        // JPA needs a no-arg constructor.
    }

    public SalaryRecord(
            Employee employee,
            BigDecimal amount,
            String currencyCode,
            LocalDate effectiveDate
    ) {
        this.employee = employee;
        this.amount = amount;
        this.currencyCode = currencyCode;
        this.effectiveDate = effectiveDate;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Employee getEmployee() { return employee; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrencyCode() { return currencyCode; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SalaryRecord other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}