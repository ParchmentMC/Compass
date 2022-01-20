package org.parchmentmc.compass.data.sanitation.impl;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.data.sanitation.Sanitizer;
import org.parchmentmc.compass.util.DescriptorIndexer;
import org.parchmentmc.feather.mapping.MappingDataContainer;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;
import org.parchmentmc.feather.metadata.SourceMetadata;

import java.util.BitSet;

import static org.parchmentmc.feather.mapping.MappingDataContainer.ClassData;
import static org.parchmentmc.feather.mapping.MappingDataContainer.MethodData;
import static org.parchmentmc.feather.mapping.MappingDataContainer.ParameterData;

public class DescriptorParametersSanitizer extends Sanitizer {
    public DescriptorParametersSanitizer() {
        super("descriptor parameter indexes");
    }

    @MonotonicNonNull
    private DescriptorIndexer indexer = null;

    @Override
    public boolean visit(MappingDataContainer container, @Nullable SourceMetadata metadata) {
        indexer = new DescriptorIndexer();
        return super.visit(container, metadata);
    }

    @Override
    public Action<ParameterData> modifyParameter(ClassData classData, MethodData methodData, ParameterData paramData,
                                                 @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        final BitSet indexes = indexer.getIndexes(methodData, methodMetadata);

        if (!indexes.get(paramData.getIndex())) {
            return Action.delete();
        }

        return Action.nothing();
    }

    @Override
    public boolean revisit() {
        indexer = null;
        return super.revisit();
    }
}
