package org.parchmentmc.compass.data.sanitation;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * An abstract base class for sanitizers, for extending by implementors.
 */
public abstract class AbstractSanitizer implements Sanitizer {
    private final String name;

    protected AbstractSanitizer(String name) {
        this.name = name;
    }

    @Override
    @NonNull
    public final String getName() {
        return name;
    }
}
