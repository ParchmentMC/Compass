package org.parchmentmc.compass.validation;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * An abstract base class for validators, for extending by implementors.
 */
public abstract class AbstractValidator implements Validator {
    private final String name;

    protected AbstractValidator(String name) {
        this.name = name;
    }

    @Override
    @NonNull
    public final String getName() {
        return name;
    }

    protected ValidationIssue.ValidationError error(String message) {
        return new ValidationIssue.ValidationError(this, message);
    }

    protected ValidationIssue.ValidationWarning warning(String message) {
        return new ValidationIssue.ValidationWarning(this, message);
    }
}
