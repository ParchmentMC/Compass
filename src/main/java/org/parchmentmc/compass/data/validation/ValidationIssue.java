package org.parchmentmc.compass.data.validation;

public abstract class ValidationIssue {
    private final String validatorName;
    protected final String message;

    protected ValidationIssue(Validator validator, String message) {
        this.validatorName = validator.getName();
        this.message = message;
    }

    public final String getValidatorName() {
        return validatorName;
    }

    public String getMessage() {
        return message;
    }

    public static class ValidationWarning extends ValidationIssue {
        public ValidationWarning(Validator validator, String message) {
            super(validator, message);
        }
    }

    public static class ValidationError extends ValidationIssue {
        public ValidationError(Validator validator, String message) {
            super(validator, message);
        }
    }
}
