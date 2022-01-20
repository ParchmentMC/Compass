package org.parchmentmc.compass.data.validation.impl;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.data.validation.Validator;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;

import static org.parchmentmc.feather.mapping.MappingDataContainer.ClassData;
import static org.parchmentmc.feather.mapping.MappingDataContainer.MethodData;

/**
 * Validates that methods follow the current mapping standards.
 *
 * <p>There is currently one standard checked by this validator:</p>
 * <ol>
 *     <li>The methods javadoc does not contain the {@code @param} specification.</li>
 * </ol>
 */
public class MethodStandardsValidator extends Validator {
    public MethodStandardsValidator() {
        super("Method standards");
    }

    @Override
    public boolean visitMethod(ClassData classData, MethodData methodData,
                               @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        this.validateJavadoc(methodData);
        return false;
    }

    /**
     * Validates that the methods javadoc is valid.
     * Currently only validates the the javadoc does not contains an @param entry.
     *
     * @param methodData The data to verify.
     */
    private void validateJavadoc(final MethodData methodData) {
        if (methodData.getJavadoc().stream().anyMatch(line -> line.contains("@param"))) {
            error("The javadoc information contains an @param entry.");
        }
    }
}
