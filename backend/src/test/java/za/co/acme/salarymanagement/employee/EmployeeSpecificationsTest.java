package za.co.acme.salarymanagement.employee;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies that the search/filter specifications produce the expected
 * predicates against a real PostgreSQL database.
 *
 * <p>Every test seeds its own small dataset inside a rolled-back transaction.
 * Because the shared test database may contain rows created by Postman or
 * by previous test runs (which are not visible to this test's rollback but
 * <em>are</em> visible to reads), every assertion is scoped to the rows this
 * test class seeded. The convention: employee numbers here start with
 * "T-", which is unique to this class and does not collide with real or
 * Postman-created employees.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
class EmployeeSpecificationsTest {

    /** Prefix used by every employee row this test class creates. */
    private static final String TEST_PREFIX = "T-";

    @Autowired
    private EmployeeRepository repository;

    private void seed() {
        repository.save(new Employee(
                "T-ADA", "Ada", "Lovelace",
                "ada@test.acme", "GB", "Engineering",
                EmploymentStatus.ACTIVE
        ));
        repository.save(new Employee(
                "T-GRACE", "Grace", "Hopper",
                "grace@test.acme", "US", "Engineering",
                EmploymentStatus.ACTIVE
        ));
        repository.save(new Employee(
                "T-ALAN", "Alan", "Turing",
                "alan@test.acme", "GB", "Research",
                EmploymentStatus.ON_LEAVE
        ));
    }

    /** Restricts a specification to rows this test class seeded. */
    private static Specification<Employee> onlyThisTestClass() {
        return (root, query, cb) -> cb.like(root.get("employeeNumber"), TEST_PREFIX + "%");
    }

    private static Specification<Employee> scoped(Specification<Employee> spec) {
        return spec == null ? onlyThisTestClass() : spec.and(onlyThisTestClass());
    }

    @Test
    void matchesSearchReturnsNullForBlank() {
        assertThat(EmployeeSpecifications.matchesSearch(null)).isNull();
        assertThat(EmployeeSpecifications.matchesSearch("   ")).isNull();
    }

    @Test
    void matchesSearchFindsByFirstNameCaseInsensitive() {
        seed();
        Specification<Employee> spec = scoped(EmployeeSpecifications.matchesSearch("ADA"));

        assertThat(repository.findAll(spec))
                .extracting(Employee::getFirstName)
                .containsExactly("Ada");
    }

    @Test
    void matchesSearchFindsByEmailSubstring() {
        seed();
        Specification<Employee> spec = scoped(EmployeeSpecifications.matchesSearch("grace@"));

        assertThat(repository.findAll(spec))
                .extracting(Employee::getEmployeeNumber)
                .containsExactly("T-GRACE");
    }

    @Test
    void hasCountryFiltersByUppercaseCode() {
        seed();
        Specification<Employee> spec = scoped(EmployeeSpecifications.hasCountry("gb"));

        assertThat(repository.findAll(spec))
                .extracting(Employee::getEmployeeNumber)
                .containsExactlyInAnyOrder("T-ADA", "T-ALAN");
    }

    @Test
    void hasDepartmentMatchesCaseInsensitive() {
        seed();
        Specification<Employee> spec = scoped(EmployeeSpecifications.hasDepartment("engineering"));

        assertThat(repository.findAll(spec))
                .extracting(Employee::getEmployeeNumber)
                .containsExactlyInAnyOrder("T-ADA", "T-GRACE");
    }

    @Test
    void hasStatusFiltersByEnum() {
        seed();
        Specification<Employee> spec = scoped(EmployeeSpecifications.hasStatus(EmploymentStatus.ON_LEAVE));

        assertThat(repository.findAll(spec))
                .extracting(Employee::getEmployeeNumber)
                .containsExactly("T-ALAN");
    }

    @Test
    void fromCriteriaCombinesWithAnd() {
        seed();
        EmployeeSearchCriteria criteria = new EmployeeSearchCriteria(
                null, "GB", "Engineering", EmploymentStatus.ACTIVE
        );

        assertThat(repository.findAll(scoped(EmployeeSpecifications.fromCriteria(criteria))))
                .extracting(Employee::getEmployeeNumber)
                .containsExactly("T-ADA");
    }

    @Test
    void fromCriteriaReturnsNullWhenEmpty() {
        EmployeeSearchCriteria empty = new EmployeeSearchCriteria(null, null, null, null);
        assertThat(EmployeeSpecifications.fromCriteria(empty)).isNull();
    }

    @Test
    void fromCriteriaFindsBySearchAndFilters() {
        seed();
        EmployeeSearchCriteria criteria = new EmployeeSearchCriteria(
                "ada", null, null, null
        );

        Optional<Employee> result = repository
                .findAll(scoped(EmployeeSpecifications.fromCriteria(criteria)))
                .stream()
                .findFirst();

        assertThat(result).isPresent();
        assertThat(result.get().getEmployeeNumber()).isEqualTo("T-ADA");
    }
}