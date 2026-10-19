package za.co.acme.salarymanagement.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import za.co.acme.salarymanagement.employee.EmploymentStatus;

/**
 * Incoming payload for creating an employee. All fields are required.
 * Validation errors return 400 with the field name and message.
 */
public record EmployeeRequest(

        @NotBlank
        @Size(max = 20)
        String employeeNumber,

        @NotBlank
        @Size(max = 100)
        String firstName,

        @NotBlank
        @Size(max = 100)
        String lastName,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Pattern(regexp = "^[A-Z]{2}$", message = "must be a 2-letter uppercase ISO 3166-1 alpha-2 country code")
        String countryCode,

        @NotBlank
        @Size(max = 100)
        String department,

        @NotNull
        EmploymentStatus employmentStatus
) {
}