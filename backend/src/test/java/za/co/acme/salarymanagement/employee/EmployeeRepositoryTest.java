package za.co.acme.salarymanagement.employee;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository repository;

    @Test
    void savesAndRetrievesEmployeeById() {
        Employee saved = repository.save(new Employee(
                "EMP-0001", "Ada", "Lovelace",
                "ada.lovelace@acme.test", "GB", "Engineering",
                EmploymentStatus.ACTIVE
        ));

        Optional<Employee> found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getEmployeeNumber()).isEqualTo("EMP-0001");
        assertThat(found.get().getEmail()).isEqualTo("ada.lovelace@acme.test");
        assertThat(found.get().getCreatedAt()).isNotNull();
        assertThat(found.get().getUpdatedAt()).isNotNull();
    }

    @Test
    void findsByEmployeeNumber() {
        repository.save(new Employee(
                "EMP-0002", "Grace", "Hopper",
                "grace.hopper@acme.test", "US", "Engineering",
                EmploymentStatus.ACTIVE
        ));

        Optional<Employee> found = repository.findByEmployeeNumber("EMP-0002");

        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("Grace");
    }

    @Test
    void findsByEmail() {
        repository.save(new Employee(
                "EMP-0003", "Alan", "Turing",
                "alan.turing@acme.test", "GB", "Research",
                EmploymentStatus.ON_LEAVE
        ));

        Optional<Employee> found = repository.findByEmail("alan.turing@acme.test");

        assertThat(found).isPresent();
        assertThat(found.get().getLastName()).isEqualTo("Turing");
    }

    @Test
    void existsByEmployeeNumberReturnsTrueForExisting() {
        repository.save(new Employee(
                "EMP-0004", "Katherine", "Johnson",
                "katherine.johnson@acme.test", "US", "Research",
                EmploymentStatus.ACTIVE
        ));

        assertThat(repository.existsByEmployeeNumber("EMP-0004")).isTrue();
        assertThat(repository.existsByEmployeeNumber("EMP-9999")).isFalse();
    }

    @Test
    void existsByEmailReturnsFalseForUnknown() {
        assertThat(repository.existsByEmail("nobody@acme.test")).isFalse();
    }
}