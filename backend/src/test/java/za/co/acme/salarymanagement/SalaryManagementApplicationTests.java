package za.co.acme.salarymanagement;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SalaryManagementApplicationTests {

	@Test
	void contextLoads() {
		// Fails if Spring cannot wire the application context,
		// Flyway cannot run migrations, or Hibernate validation fails.
	}
}