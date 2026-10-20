package za.co.acme.salarymanagement.employee;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import tools.jackson.databind.ObjectMapper;
import za.co.acme.salarymanagement.common.EmployeeNotFoundException;
import za.co.acme.salarymanagement.common.GlobalExceptionHandler;
import za.co.acme.salarymanagement.employee.dto.EmployeeRequest;
import za.co.acme.salarymanagement.employee.dto.EmployeeResponse;

@WebMvcTest(controllers = EmployeeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, MethodValidationPostProcessor.class})
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmployeeService service;

    // --- create ---

    @Test
    void createReturns201WithBody() throws Exception {
        EmployeeResponse response = sampleResponse(1L, "EMP-1000", "ada@acme.test");
        when(service.create(any(EmployeeRequest.class))).thenReturn(response);

        String body = objectMapper.writeValueAsString(new EmployeeRequest(
                "EMP-1000", "Ada", "Lovelace", "ada@acme.test",
                "GB", "Engineering", EmploymentStatus.ACTIVE
        ));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.employeeNumber").value("EMP-1000"))
                .andExpect(jsonPath("$.email").value("ada@acme.test"))
                .andExpect(jsonPath("$.countryCode").value("GB"));
    }

    @Test
    void createReturns400WhenValidationFails() throws Exception {
        String body = """
                {
                  "employeeNumber": "",
                  "firstName": "Ada",
                  "lastName": "Lovelace",
                  "email": "not-an-email",
                  "countryCode": "gb",
                  "department": "Engineering",
                  "employmentStatus": "ACTIVE"
                }
                """;

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").isArray());
    }

    // --- get by id ---

    @Test
    void getByIdReturnsEmployee() throws Exception {
        when(service.getById(1L)).thenReturn(sampleResponse(1L, "EMP-1000", "ada@acme.test"));

        mockMvc.perform(get("/api/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.employeeNumber").value("EMP-1000"));
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        when(service.getById(999L)).thenThrow(new EmployeeNotFoundException(999L));

        mockMvc.perform(get("/api/employees/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee not found with id: 999"));
    }

    // --- list (unfiltered) ---

    @Test
    void listReturnsPage() throws Exception {
        Page<EmployeeResponse> page = new PageImpl<>(
                List.of(sampleResponse(1L, "EMP-1000", "ada@acme.test")),
                PageRequest.of(0, 20),
                1
        );
        when(service.list(any(EmployeeSearchCriteria.class), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/employees").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].employeeNumber").value("EMP-1000"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // --- list (filtered) ---

    @Test
    void listPassesSearchAndFiltersToService() throws Exception {
        Page<EmployeeResponse> page = new PageImpl<>(
                List.of(sampleResponse(1L, "EMP-1000", "ada@acme.test")),
                PageRequest.of(0, 10),
                1
        );
        when(service.list(any(EmployeeSearchCriteria.class), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/employees")
                        .param("search", "ada")
                        .param("country", "GB")
                        .param("department", "Engineering")
                        .param("status", "ACTIVE")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "lastName,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].employeeNumber").value("EMP-1000"));
    }

    @Test
    void listReturns400WhenStatusIsInvalid() throws Exception {
        mockMvc.perform(get("/api/employees").param("status", "BANANA"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listReturns400WhenSizeExceedsCap() throws Exception {
        mockMvc.perform(get("/api/employees").param("size", "500"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listReturns400WhenSortFieldIsNotWhitelisted() throws Exception {
        mockMvc.perform(get("/api/employees").param("sort", "passwordHash,asc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listReturns400WhenCountryFormatIsInvalid() throws Exception {
        mockMvc.perform(get("/api/employees").param("country", "GBR"))
                .andExpect(status().isBadRequest());
    }

    // --- helper ---

    private EmployeeResponse sampleResponse(Long id, String employeeNumber, String email) {
        OffsetDateTime now = OffsetDateTime.now();
        return new EmployeeResponse(
                id,
                employeeNumber,
                "Ada",
                "Lovelace",
                email,
                "GB",
                "Engineering",
                EmploymentStatus.ACTIVE,
                now,
                now
        );
    }
}