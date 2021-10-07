package org.parchmentmc.compass.data.validation.impl;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.data.validation.AbstractValidator;
import org.parchmentmc.compass.data.validation.ValidationIssue;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;

import java.util.function.Consumer;

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
public class MethodStandardsValidator extends AbstractValidator {
    public MethodStandardsValidator() {
        super("Method standards");
    }

    @Override
    public void validate(Consumer<? super ValidationIssue> issues, ClassData classData, MethodData methodData,
                         @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        this.validateJavadoc(issues, methodData);
    }

    /**
     * Validates that the methods javadoc is valid.
     * Currently only validates the the javadoc does not contains an @param entry.
     *
     * @param issues     A consumer of issues
     * @param methodData The data to verify.
     */
    private void validateJavadoc(final Consumer<? super ValidationIssue> issues, final MethodData methodData) {
        if (methodData.getJavadoc().stream().anyMatch(line -> line.contains("@param"))) {
            issues.accept(error("The javadoc information contains an @param entry."));
        }
    }
}
