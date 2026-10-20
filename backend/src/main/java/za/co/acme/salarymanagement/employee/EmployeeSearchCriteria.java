package za.co.acme.salarymanagement.employee;

/**
 * Validated search and filter values, already normalised by the controller.
 * Passed from the controller to the service. Deliberately a record: it is a
 * value object, carries no behaviour, and is only valid when constructed
 * through the controller's validated parameters.
 */
public record EmployeeSearchCriteria(
        String search,
        String countryCode,
        String department,
        EmploymentStatus status
) {
    /**
     * True when no filter is active. Used by the service to skip the
     * specification entirely and use a plain {@code findAll(pageable)}.
     * This is a small optimisation, not a correctness requirement.
     */
    public boolean isEmpty() {
        return search == null && countryCode == null
                && department == null && status == null;
    }
}