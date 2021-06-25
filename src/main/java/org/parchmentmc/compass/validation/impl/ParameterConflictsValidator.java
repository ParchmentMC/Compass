package org.parchmentmc.compass.validation.impl;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.validation.AbstractValidator;
import org.parchmentmc.compass.validation.ValidationIssue;
import org.parchmentmc.feather.mapping.MappingDataContainer;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Validates that parameter names do not conflict.
 */
public class ParameterConflictsValidator extends AbstractValidator {
    // Reused to avoid creating a multi-map for each method being validated
    private final Multimap<String, Byte> reusedNameToIndex = MultimapBuilder.hashKeys().treeSetValues().build();

    public ParameterConflictsValidator() {
        super("parameter name conflicts");
    }

    @Override
    public List<? extends ValidationIssue> validate(MappingDataContainer.ClassData classData, MappingDataContainer.MethodData methodData, @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        reusedNameToIndex.clear();
        for (MappingDataContainer.ParameterData params : methodData.getParameters()) {
            if (params.getName() != null) { // Only checked named parameters
                reusedNameToIndex.put(params.getName(), params.getIndex());
            }
        }

        List<ValidationIssue.ValidationError> errors = null;
        for (String name : reusedNameToIndex.keySet()) {
            final Collection<Byte> indices = reusedNameToIndex.get(name);
            if (indices.size() > 1) { // Conflicts! weewoo *insert patrick star meme here*
                ValidationIssue.ValidationError error = error("Parameters at indices " + indices + " conflict with same name: " + name);

                // The following ensures we make only the list as needed
                if (errors == null) {
                    errors = Collections.singletonList(error);
                } else if (errors.size() == 1) {
                    errors = new ArrayList<>(errors);
                    errors.add(error);
                } else {
                    errors.add(error);
                }
            }
        }

        return errors != null ? errors : Collections.emptyList();
    }
}
