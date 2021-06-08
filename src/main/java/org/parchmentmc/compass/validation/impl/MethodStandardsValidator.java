package org.parchmentmc.compass.validation.impl;

import com.google.common.collect.Lists;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.validation.AbstractValidator;
import org.parchmentmc.compass.validation.ValidationIssue;
import org.parchmentmc.feather.mapping.MappingDataContainer;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;

import java.util.List;

/**
 * Validates that methods follow the current mapping standards.
 *
 * <p>There is currently one standard checked by this validator:</p>
 * <ol>
 *     <li>The methods javadoc does not contain the {@code @param} specification.</li>
 * </ol>
 */
public class MethodStandardsValidator extends AbstractValidator
{
    public MethodStandardsValidator()
    {
        super("Method standards");
    }

    @Override
    public List<? extends ValidationIssue> validate(
      final MappingDataContainer.ClassData classData,
      final MappingDataContainer.MethodData methodData,
      @Nullable final ClassMetadata classMetadata,
      @Nullable final MethodMetadata methodMetadata)
    {
        final List<ValidationIssue> issues = Lists.newArrayList();

        this.validateJavadoc(issues, methodData);

        return issues;
    }

    /**
     * Validates that the methods javadoc is valid.
     * Currently only validates the the javadoc does not contains an @param entry.
     *
     * @param issues The list of issues to return.
     * @param methodData The data to verify.
     */
    private void validateJavadoc(final List<ValidationIssue> issues, final MappingDataContainer.MethodData methodData)
    {
        if (methodData.getJavadoc().stream().anyMatch(
          line -> line.contains("@param")
        )) {
            issues.add(error("The javadoc information contains an @param entry."));
        }
    }
}
