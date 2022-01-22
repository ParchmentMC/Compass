package org.parchmentmc.compass.data.sanitation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.data.visitation.ModifyingDataVisitor;
import org.parchmentmc.feather.mapping.MappingDataBuilder;
import org.parchmentmc.feather.mapping.MappingDataContainer;
import org.parchmentmc.feather.metadata.SourceMetadata;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.parchmentmc.feather.mapping.MappingDataBuilder.copyOf;

/**
 * A data sanitizer, which runs multiple {@link Sanitizer} on given input mapping data.
 *
 * <p>When sanitizing input data, all sanitizers are run serially on the data, in the order of their registration. A
 * sanitizer may request to run multiple passes against the data through the {@link Sanitizer#revisit()} method, however
 * there is a limit on how many revisits a sanitizer may request -- any further requests to revisit past that limit is
 * ignored.</p>
 */
public class DataSanitizer {
    private final Set<Sanitizer> sanitizers = new LinkedHashSet<>();
    private final int revisitLimit;

    public DataSanitizer() {
        this(5);
    }

    public DataSanitizer(int revisitLimit) {
        this.revisitLimit = revisitLimit;
    }

    public void addSanitizer(Sanitizer sanitizer) {
        sanitizers.add(sanitizer);
    }

    public boolean removeSanitizer(Sanitizer sanitizer) {
        return sanitizers.remove(sanitizer);
    }

    public Set<Sanitizer> getSanitizers() {
        return Collections.unmodifiableSet(sanitizers);
    }

    /**
     * Sanitizes the given input data and returns a copy of the sanitized data.
     *
     * <p>Most sanitizers are expected to make use of the source metadata, and therefore sanitation may not work
     * as expected if the metadata is not provided.</p>
     *
     * @param inputData the data to be sanitized
     * @param metadata  the metadata, may be {@code null}
     * @return the sanitized data
     */
    public MappingDataContainer sanitize(MappingDataContainer inputData, @Nullable SourceMetadata metadata) {
        final MappingDataBuilder workingData = copyOf(inputData);

        for (Sanitizer sanitizer : sanitizers) {
            ModifyingDataVisitor.visit(revisitLimit, sanitizer, workingData, metadata);
        }

        return workingData;
    }
}
