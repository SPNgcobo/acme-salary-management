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
 * predicates against a real PostgreSQL database. Each test seeds a small
 * dataset inside a rolled-back transaction, so runs are independent of one
 * another and of any manual data in the shared database.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
class EmployeeSpecificationsTest {

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

    @Test
    void matchesSearchReturnsNullForBlank() {
        assertThat(EmployeeSpecifications.matchesSearch(null)).isNull();
        assertThat(EmployeeSpecifications.matchesSearch("   ")).isNull();
    }

    @Test
    void matchesSearchFindsByFirstNameCaseInsensitive() {
        seed();
        Specification<Employee> spec = EmployeeSpecifications.matchesSearch("ADA");

        assertThat(repository.findAll(spec))
                .extracting(Employee::getFirstName)
                .containsExactly("Ada");
    }

    @Test
    void matchesSearchFindsByEmailSubstring() {
        seed();
        Specification<Employee> spec = EmployeeSpecifications.matchesSearch("grace@");

        assertThat(repository.findAll(spec))
                .extracting(Employee::getEmployeeNumber)
                .containsExactly("T-GRACE");
    }

    @Test
    void hasCountryFiltersByUppercaseCode() {
        seed();
        Specification<Employee> spec = EmployeeSpecifications.hasCountry("gb");

        assertThat(repository.findAll(spec))
                .extracting(Employee::getEmployeeNumber)
                .containsExactlyInAnyOrder("T-ADA", "T-ALAN");
    }

    @Test
    void hasDepartmentMatchesCaseInsensitive() {
        seed();
        Specification<Employee> spec = EmployeeSpecifications.hasDepartment("engineering");

        assertThat(repository.findAll(spec))
                .extracting(Employee::getEmployeeNumber)
                .containsExactlyInAnyOrder("T-ADA", "T-GRACE");
    }

    @Test
    void hasStatusFiltersByEnum() {
        seed();
        Specification<Employee> spec = EmployeeSpecifications.hasStatus(EmploymentStatus.ON_LEAVE);

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

        assertThat(repository.findAll(EmployeeSpecifications.fromCriteria(criteria)))
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
                .findAll(EmployeeSpecifications.fromCriteria(criteria))
                .stream()
                .findFirst();

        assertThat(result).isPresent();
        assertThat(result.get().getEmployeeNumber()).isEqualTo("T-ADA");
    }
}