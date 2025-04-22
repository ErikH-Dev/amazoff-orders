package exceptions;

import java.util.List;

public class ValidationErrorResponse extends ErrorResponse {
    private List<Violation> violations;

    public ValidationErrorResponse(List<Violation> violations) {
        super("Validation error");
        this.violations = violations;
    }

    public List<Violation> getViolations() {
        return violations;
    }

    public void setViolations(List<Violation> violations) {
        this.violations = violations;
    }
}