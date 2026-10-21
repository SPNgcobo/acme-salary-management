package za.co.acme.salarymanagement.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.acme.salarymanagement.employee.Employee;

/**
 * Read-only aggregation queries for reporting.
 *
 * <p>The queries are native SQL rather than JPQL because they involve
 * {@code DISTINCT ON} and {@code width_bucket}, which do not have direct
 * JPA equivalents and which Postgres executes far more efficiently than
 * a Java-side rewrite would.
 *
 * <p>Every query filters to the "current" salary per employee: the record
 * with the highest {@code effective_date} on or before the given date
 * (see Decision 19). The {@code current_salaries} CTE with
 * {@code DISTINCT ON (employee_id)} is the idiomatic Postgres expression
 * of that rule and uses the {@code idx_salary_records_employee_effective_desc}
 * index directly.
 *
 * <p>The type parameter is {@link Employee} because
 * {@code JpaRepository<Employee, Long>} is the minimum viable base class
 * for Spring Data to wire the bean; none of the inherited CRUD methods are
 * used. A dedicated interface extending {@code Repository<Employee, Long>}
 * would also work, but using {@code JpaRepository} avoids a corner case in
 * older Spring Data wiring when only interface methods are declared.
 */
@Repository
public interface ReportRepository extends JpaRepository<Employee, Long> {

    /**
     * Total employee count, independent of salary state. Employees with no
     * salary record still count.
     */
    @Query(value = "select count(*) from employees", nativeQuery = true)
    long countAllEmployees();

    /**
     * Number of distinct countries represented in the employee table.
     */
    @Query(value = "select count(distinct country_code) from employees", nativeQuery = true)
    long countDistinctCountries();

    /**
     * Number of distinct departments represented in the employee table.
     */
    @Query(value = "select count(distinct department) from employees", nativeQuery = true)
    long countDistinctDepartments();

    /**
     * Number of employees with at least one salary record effective on or
     * before {@code onDate}. Excludes employees whose only salary records
     * are future-dated.
     */
    @Query(value = """
            select count(distinct sr.employee_id)
            from salary_records sr
            where sr.effective_date <= :onDate
            """, nativeQuery = true)
    long countEmployeesWithCurrentSalary(@Param("onDate") LocalDate onDate);

    /**
     * Per-currency totals across all currently-effective salaries.
     *
     * <p>Returns one row per currency. Each row aggregates only salaries
     * in that currency — the report deliberately does not attempt a
     * cross-currency total (see Decision 10).
     *
     * <p>The projection uses a {@code Object[]} because native queries
     * cannot map to record types without a result transformer; the service
     * is the single place where the array is mapped to a typed DTO.
     *
     * <p>Column order in each row: {@code currency_code, total_amount,
     * employee_count, average_amount}.
     */
    @Query(value = """
            with current_salaries as (
                select distinct on (sr.employee_id)
                    sr.employee_id,
                    sr.amount,
                    sr.currency_code
                from salary_records sr
                where sr.effective_date <= :onDate
                order by sr.employee_id, sr.effective_date desc
            )
            select
                cs.currency_code,
                sum(cs.amount) as total_amount,
                count(*) as employee_count,
                round(avg(cs.amount), 2) as average_amount
            from current_salaries cs
            group by cs.currency_code
            order by cs.currency_code
            """, nativeQuery = true)
    List<Object[]> salaryTotalsByCurrency(@Param("onDate") LocalDate onDate);

    /**
     * Salary aggregates grouped by country for one currency.
     *
     * <p>Returns rows of the form:
     * {@code [country_code, employee_count, min_amount, max_amount, average_amount]}.
     */
    @Query(value = """
            with current_salaries as (
                select distinct on (sr.employee_id)
                    sr.employee_id,
                    sr.amount,
                    sr.currency_code
                from salary_records sr
                where sr.effective_date <= :onDate
                  and sr.currency_code = :currency
                order by sr.employee_id, sr.effective_date desc
            )
            select
                e.country_code,
                count(*) as employee_count,
                min(cs.amount) as min_amount,
                max(cs.amount) as max_amount,
                round(avg(cs.amount), 2) as average_amount
            from current_salaries cs
            join employees e on e.id = cs.employee_id
            group by e.country_code
            order by e.country_code
            """, nativeQuery = true)
    List<Object[]> salaryByCountry(
            @Param("onDate") LocalDate onDate,
            @Param("currency") String currency
    );

    /**
     * Salary aggregates grouped by department for one currency.
     *
     * <p>Returns rows of the form:
     * {@code [department, employee_count, min_amount, max_amount, average_amount]}.
     */
    @Query(value = """
            with current_salaries as (
                select distinct on (sr.employee_id)
                    sr.employee_id,
                    sr.amount,
                    sr.currency_code
                from salary_records sr
                where sr.effective_date <= :onDate
                  and sr.currency_code = :currency
                order by sr.employee_id, sr.effective_date desc
            )
            select
                e.department,
                count(*) as employee_count,
                min(cs.amount) as min_amount,
                max(cs.amount) as max_amount,
                round(avg(cs.amount), 2) as average_amount
            from current_salaries cs
            join employees e on e.id = cs.employee_id
            group by e.department
            order by e.department
            """, nativeQuery = true)
    List<Object[]> salaryByDepartment(
            @Param("onDate") LocalDate onDate,
            @Param("currency") String currency
    );

    /**
     * Maximum current salary in the given currency. Used by the service to
     * size the distribution histogram. Returns {@code null} if no current
     * salary exists in that currency.
     */
    @Query(value = """
            with current_salaries as (
                select distinct on (sr.employee_id)
                    sr.employee_id,
                    sr.amount,
                    sr.currency_code
                from salary_records sr
                where sr.effective_date <= :onDate
                  and sr.currency_code = :currency
                order by sr.employee_id, sr.effective_date desc
            )
            select max(cs.amount) from current_salaries cs
            """, nativeQuery = true)
    BigDecimal maxCurrentSalary(
            @Param("onDate") LocalDate onDate,
            @Param("currency") String currency
    );

    /**
     * Salary distribution histogram for one currency. Uses Postgres
     * {@code width_bucket} to assign each current salary to a bucket of
     * {@code bucketSize}, then counts.
     *
     * <p>All parameters are cast with explicit {@code CAST(... AS ...)}.
     * Two reasons:
     * <ol>
     *   <li>Postgres's overload resolution requires
     *   {@code width_bucket(numeric, numeric, numeric, integer)} — without
     *   casts, the binding produces {@code (numeric, integer, numeric, bigint)}
     *   and no overload matches.</li>
     *   <li>Hibernate's named-parameter parser does not handle the {@code ::}
     *   cast operator on a bound parameter — it reads
     *   {@code :bucketCount::integer} as a parameter named
     *   {@code bucketCount::integer}. The {@code CAST(... AS ...)} form
     *   terminates the token cleanly and binds correctly.</li>
     * </ol>
     */
    @Query(value = """
            with current_salaries as (
                select distinct on (sr.employee_id)
                    sr.employee_id,
                    sr.amount,
                    sr.currency_code
                from salary_records sr
                where sr.effective_date <= :onDate
                  and sr.currency_code = :currency
                order by sr.employee_id, sr.effective_date desc
            ),
            bucketed as (
                select width_bucket(
                    cast(cs.amount as numeric),
                    cast(0 as numeric),
                    cast(:bucketSize as numeric) * cast(:bucketCount as numeric),
                    cast(:bucketCount as integer)
                ) - 1 as bucket
                from current_salaries cs
            )
            select
                b.bucket * cast(:bucketSize as numeric) as lower_bound_inclusive,
                (b.bucket + 1) * cast(:bucketSize as numeric) as upper_bound_exclusive,
                count(*) as employee_count
            from bucketed b
            group by b.bucket
            order by b.bucket
            """, nativeQuery = true)
    List<Object[]> salaryDistribution(
            @Param("onDate") LocalDate onDate,
            @Param("currency") String currency,
            @Param("bucketSize") BigDecimal bucketSize,
            @Param("bucketCount") long bucketCount
    );

    /**
     * Top-earners report for one currency.
     *
     * <p>Returns rows of the form:
     * {@code [employee_id, employee_number, first_name, last_name,
     * department, country_code, amount, currency_code, effective_date]}.
     */
    @Query(value = """
            with current_salaries as (
                select distinct on (sr.employee_id)
                    sr.employee_id,
                    sr.amount,
                    sr.currency_code,
                    sr.effective_date
                from salary_records sr
                where sr.effective_date <= :onDate
                  and sr.currency_code = :currency
                order by sr.employee_id, sr.effective_date desc
            )
            select
                e.id as employee_id,
                e.employee_number,
                e.first_name,
                e.last_name,
                e.department,
                e.country_code,
                cs.amount,
                cs.currency_code,
                cs.effective_date
            from current_salaries cs
            join employees e on e.id = cs.employee_id
            order by cs.amount desc, e.id asc
            limit :limit
            """, nativeQuery = true)
    List<Object[]> topEarners(
            @Param("onDate") LocalDate onDate,
            @Param("currency") String currency,
            @Param("limit") int limit
    );
}