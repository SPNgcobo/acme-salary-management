package za.co.acme.salarymanagement.employee;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * Composable JPA specifications for employee search and filtering.
 *
 * <p>Each method returns an independent specification that can be combined
 * with {@code .and()} or {@code .or()}. Combinations are evaluated by
 * PostgreSQL, not by the JVM, so only the requested page is loaded into
 * memory.
 *
 * <p>Search semantics (see docs/design-decisions.md Decision 15): a
 * free-text term matches if any of first_name, last_name, employee_number,
 * or email contains the term as a case-insensitive substring. Filters
 * (country, department, status) are exact matches after normalisation and
 * combine with the search via AND.
 */
public final class EmployeeSpecifications {

    private EmployeeSpecifications() {
        // utility class
    }

    /**
     * Free-text search across first name, last name, employee number, and
     * email. Case-insensitive partial match. Null or blank term is treated
     * as "no search" and returns a specification that matches everything.
     */
    public static Specification<Employee> matchesSearch(String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return null;
        }
        String pattern = "%" + searchTerm.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            Predicate firstName = cb.like(cb.lower(root.get("firstName")), pattern);
            Predicate lastName = cb.like(cb.lower(root.get("lastName")), pattern);
            Predicate employeeNumber = cb.like(cb.lower(root.get("employeeNumber")), pattern);
            Predicate email = cb.like(cb.lower(root.get("email")), pattern);
            return cb.or(firstName, lastName, employeeNumber, email);
        };
    }

    /**
     * Exact match on country code (assumed already uppercase). Null returns
     * a specification that matches everything.
     */
    public static Specification<Employee> hasCountry(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return null;
        }
        String upper = countryCode.trim().toUpperCase();
        return (root, query, cb) -> cb.equal(root.get("countryCode"), upper);
    }

    /**
     * Case-insensitive exact match on department. Null returns a
     * specification that matches everything.
     */
    public static Specification<Employee> hasDepartment(String department) {
        if (department == null || department.isBlank()) {
            return null;
        }
        String lower = department.trim().toLowerCase();
        return (root, query, cb) -> cb.equal(cb.lower(root.get("department")), lower);
    }

    /**
     * Exact match on employment status. Null returns a specification that
     * matches everything.
     */
    public static Specification<Employee> hasStatus(EmploymentStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("employmentStatus"), status);
    }

    /**
     * Combines all provided criteria into a single specification. Any
     * criteria that are null are skipped. If everything is null or blank,
     * returns null so the caller can fall back to {@code findAll(pageable)}.
     */
    public static Specification<Employee> fromCriteria(EmployeeSearchCriteria criteria) {
        if (criteria == null || criteria.isEmpty()) {
            return null;
        }

        List<Specification<Employee>> specs = new ArrayList<>();
        addIfPresent(specs, matchesSearch(criteria.search()));
        addIfPresent(specs, hasCountry(criteria.countryCode()));
        addIfPresent(specs, hasDepartment(criteria.department()));
        addIfPresent(specs, hasStatus(criteria.status()));

        if (specs.isEmpty()) {
            return null;
        }

        // Combine with AND
        Specification<Employee> combined = specs.get(0);
        for (int i = 1; i < specs.size(); i++) {
            combined = combined.and(specs.get(i));
        }
        return combined;
    }

    private static void addIfPresent(
            List<Specification<Employee>> specs,
            Specification<Employee> spec
    ) {
        if (spec != null) {
            specs.add(spec);
        }
    }
}