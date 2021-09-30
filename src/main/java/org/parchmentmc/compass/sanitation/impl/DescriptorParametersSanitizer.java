package org.parchmentmc.compass.sanitation.impl;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.sanitation.AbstractSanitizer;
import org.parchmentmc.compass.util.DescriptorIndexer;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;

import java.util.BitSet;

import static org.parchmentmc.feather.mapping.MappingDataContainer.*;

public class DescriptorParametersSanitizer extends AbstractSanitizer {
    public DescriptorParametersSanitizer() {
        super("descriptor parameter indexes");
    }

    @MonotonicNonNull
    private DescriptorIndexer indexer = null;

    @Override
    public boolean start(boolean isMetadataAvailable) {
        indexer = new DescriptorIndexer();
        return super.start(isMetadataAvailable);
    }

    @Override
    public Action<ParameterData> sanitize(ClassData classData, MethodData methodData, ParameterData paramData,
                                          @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        final BitSet indexes = indexer.getIndexes(methodData, methodMetadata);

        if (!indexes.get(paramData.getIndex())) {
            return Action.delete();
        }

        return Action.nothing();
    }
}
