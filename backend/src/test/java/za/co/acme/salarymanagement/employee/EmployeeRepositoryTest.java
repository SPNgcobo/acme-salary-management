package za.co.acme.salarymanagement.employee;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository repository;

    /**
     * Unique data per test. The tests run against real Postgres (see
     * Decision 12 in docs/design-decisions.md). Combined with @Transactional
     * above, each test method runs in its own transaction and rolls back at
     * the end — so no state leaks between tests or into the shared database.
     */
    private static String uniqueEmployeeNumber() {
        return "T-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private static String uniqueEmail(String employeeNumber) {
        return employeeNumber.toLowerCase() + "@test.acme";
    }

    @Test
    void savesAndRetrievesEmployeeById() {
        String number = uniqueEmployeeNumber();
        Employee saved = repository.save(new Employee(
                number, "Ada", "Lovelace",
                uniqueEmail(number), "GB", "Engineering",
                EmploymentStatus.ACTIVE
        ));

        Optional<Employee> found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getEmployeeNumber()).isEqualTo(number);
        assertThat(found.get().getCreatedAt()).isNotNull();
        assertThat(found.get().getUpdatedAt()).isNotNull();
    }

    @Test
    void findsByEmployeeNumber() {
        String number = uniqueEmployeeNumber();
        repository.save(new Employee(
                number, "Grace", "Hopper",
                uniqueEmail(number), "US", "Engineering",
                EmploymentStatus.ACTIVE
        ));

        Optional<Employee> found = repository.findByEmployeeNumber(number);

        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("Grace");
    }

    @Test
    void findsByEmail() {
        String number = uniqueEmployeeNumber();
        String email = uniqueEmail(number);
        repository.save(new Employee(
                number, "Alan", "Turing",
                email, "GB", "Research",
                EmploymentStatus.ON_LEAVE
        ));

        Optional<Employee> found = repository.findByEmail(email);

        assertThat(found).isPresent();
        assertThat(found.get().getLastName()).isEqualTo("Turing");
    }

    @Test
    void existsByEmployeeNumberReturnsTrueForExisting() {
        String number = uniqueEmployeeNumber();
        repository.save(new Employee(
                number, "Katherine", "Johnson",
                uniqueEmail(number), "US", "Research",
                EmploymentStatus.ACTIVE
        ));

        assertThat(repository.existsByEmployeeNumber(number)).isTrue();
        assertThat(repository.existsByEmployeeNumber("NOT-" + uniqueEmployeeNumber())).isFalse();
    }

    @Test
    void existsByEmailReturnsFalseForUnknown() {
        assertThat(repository.existsByEmail(uniqueEmail(uniqueEmployeeNumber()))).isFalse();
    }
}