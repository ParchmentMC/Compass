package org.parchmentmc.compass.validation.impl;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.util.DescriptorIndexer;
import org.parchmentmc.compass.validation.AbstractValidator;
import org.parchmentmc.compass.validation.ValidationIssue;
import org.parchmentmc.feather.mapping.MappingDataContainer.ClassData;
import org.parchmentmc.feather.mapping.MappingDataContainer.MethodData;
import org.parchmentmc.feather.mapping.MappingDataContainer.ParameterData;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;

import java.util.BitSet;
import java.util.function.Consumer;

public class ParameterIndexValidator extends AbstractValidator {
    private final DescriptorIndexer indexer = new DescriptorIndexer();

    public ParameterIndexValidator() {
        super("parameter indexes");
    }

    @Override
    public void validate(Consumer<? super ValidationIssue> issues, ClassData classData, MethodData methodData,
                         ParameterData paramData, @Nullable ClassMetadata classMetadata,
                         @Nullable MethodMetadata methodMetadata) {
        final BitSet indexes = indexer.getIndexes(methodData, methodMetadata);
        byte paramIndex = paramData.getIndex();

        if (!indexes.get(paramIndex)) {
            // When above is resolved, add special-case for index 0 and non-static method
            issues.accept(error("Parameter does not exist according to descriptor"));
        }
    }
}
