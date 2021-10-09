package org.parchmentmc.compass.data.validation.impl;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.data.validation.AbstractValidator;
import org.parchmentmc.compass.data.validation.ValidationIssue;
import org.parchmentmc.feather.mapping.MappingDataContainer.ClassData;
import org.parchmentmc.feather.mapping.MappingDataContainer.MethodData;
import org.parchmentmc.feather.mapping.MappingDataContainer.ParameterData;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

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
    public void validate(Consumer<? super ValidationIssue> issueHandler, ClassData classData, MethodData methodData,
                         @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        reusedNameToIndex.clear();
        for (ParameterData params : methodData.getParameters()) {
            if (params.getName() != null) { // Only checked named parameters
                reusedNameToIndex.put(params.getName(), params.getIndex());
            }
        }

        List<ValidationIssue.ValidationError> errors = null;
        for (String name : reusedNameToIndex.keySet()) {
            final Collection<Byte> indices = reusedNameToIndex.get(name);
            if (indices.size() > 1) { // Conflicts! weewoo *insert patrick star meme here*
                issueHandler.accept(error("Parameters at indices " + indices + " conflict with same name: " + name));
            }
        }
    }
}
