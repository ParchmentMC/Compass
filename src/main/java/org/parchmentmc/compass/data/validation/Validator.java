package org.parchmentmc.compass.data.validation;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.gradle.api.Named;
import org.parchmentmc.compass.data.visitation.DataVisitor;

import java.util.function.Consumer;

/**
 * A validator for mapping data.
 *
 * <p>Validators will only pass through the mapping set <strong>once</strong>; it will not revisit the mapping set.</p>
 *
 * <p>The mapping data passed into the validators must be in official names.</p>
 *
 * <p>Implementors should override one of the {@code visit} methods and implement their validation logic.</p>
 *
 * @see org.parchmentmc.feather.mapping.MappingDataContainer
 */
public abstract class Validator implements Named, DataVisitor {
    private final String name;
    Consumer<? super ValidationIssue> issueHandler;

    protected Validator(String name) {
        this.name = name;
    }

    @Override
    @NonNull
    public final String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated Validators will never revisit the mapping data after their first pass.
     */
    @Deprecated
    @Override
    public final boolean revisit() {
        return false;
    }

    protected void error(String message) {
        if (issueHandler != null) {
            issueHandler.accept(new ValidationIssue.ValidationError(this, message));
        }
    }

    protected void warning(String message) {
        if (issueHandler != null) {
            issueHandler.accept(new ValidationIssue.ValidationWarning(this, message));
        }
    }
}
