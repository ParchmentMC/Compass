package org.parchmentmc.compass.data.validation.impl;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.data.validation.Validator;
import org.parchmentmc.compass.util.DescriptorIndexer;
import org.parchmentmc.feather.mapping.MappingDataContainer.ClassData;
import org.parchmentmc.feather.mapping.MappingDataContainer.MethodData;
import org.parchmentmc.feather.mapping.MappingDataContainer.ParameterData;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;

import java.util.BitSet;

/**
 * Validates that only the valid parameters according to the method descriptor exist.
 *
 * @see DescriptorIndexer
 */
public class ParameterIndexValidator extends Validator {
    private final DescriptorIndexer indexer = new DescriptorIndexer();

    public ParameterIndexValidator() {
        super("parameter indexes");
    }

    @Override
    public boolean preVisit(DataType type) {
        return DataType.PARAMETERS.test(type);
    }

    @Override
    public void visitParameter(ClassData classData, MethodData methodData, ParameterData paramData,
                               @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        final BitSet indexes = indexer.getIndexes(methodData, methodMetadata);
        byte paramIndex = paramData.getIndex();

        if (!indexes.get(paramIndex)) {
            // When above is resolved, add special-case for index 0 and non-static method
            error("Parameter does not exist according to descriptor");
        }
    }
}
