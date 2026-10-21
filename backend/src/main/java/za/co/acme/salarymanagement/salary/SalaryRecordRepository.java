package za.co.acme.salarymanagement.salary;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SalaryRecordRepository extends JpaRepository<SalaryRecord, Long> {

    /**
     * Verifies that a salary already exists for the given employee on the
     * given effective date. Used by the service to produce a friendly 409
     * before the database unique constraint fires.
     */
    boolean existsByEmployeeIdAndEffectiveDate(Long employeeId, LocalDate effectiveDate);

    /**
     * Current salary as of a given date — the record with the highest
     * effective_date that is on or before the given date. Uses the
     * idx_salary_records_employee_effective_desc index directly.
     */
    Optional<SalaryRecord> findTopByEmployeeIdAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
            Long employeeId, LocalDate onDate);

    /**
     * Paginated history for an employee. Sorted by the pageable's sort,
     * which defaults to effectiveDate DESC at the controller.
     */
    Page<SalaryRecord> findByEmployeeId(Long employeeId, Pageable pageable);

    /**
     * Fetches a salary record by id, eager-loading the employee so the DTO
     * mapping in the controller does not trigger a lazy-load outside the
     * service transaction.
     */
    @Query("select sr from SalaryRecord sr join fetch sr.employee where sr.id = :id")
    Optional<SalaryRecord> findByIdWithEmployee(@Param("id") Long id);
}