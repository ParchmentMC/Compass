package org.parchmentmc.compass.data.sanitation;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.gradle.api.Named;
import org.parchmentmc.compass.data.visitation.ModifyingDataVisitor;

/**
 * A sanitizer for mapping data.
 *
 * <p>The sanitize methods within this class may not return {@code null}. Instead, they must return an appropriate
 * action such as {@link Action#nothing()}</p>
 *
 * <p>The mapping data passed into the validators must be in Official names.</p>
 */
public abstract class Sanitizer implements ModifyingDataVisitor, Named {
    private final String name;

    protected Sanitizer(String name) {
        this.name = name;
    }

    @Override
    @NonNull
    public final String getName() {
        return name;
    }
}
