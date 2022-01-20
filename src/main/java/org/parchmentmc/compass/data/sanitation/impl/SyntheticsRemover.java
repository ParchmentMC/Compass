package org.parchmentmc.compass.data.sanitation.impl;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.data.sanitation.Sanitizer;
import org.parchmentmc.feather.mapping.MappingDataContainer;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.FieldMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;
import org.parchmentmc.feather.metadata.SourceMetadata;
import org.parchmentmc.feather.util.AccessFlag;

import static org.parchmentmc.feather.mapping.MappingDataContainer.ClassData;
import static org.parchmentmc.feather.mapping.MappingDataContainer.FieldData;
import static org.parchmentmc.feather.mapping.MappingDataContainer.MethodData;

/**
 * Removes synthetics fields and (non-lambda) methods.
 */
public class SyntheticsRemover extends Sanitizer {
    public SyntheticsRemover() {
        super("synthetics");
    }

    @Override
    public boolean visit(MappingDataContainer container, @Nullable SourceMetadata metadata) {
        return metadata != null; // Skip if metadata is not available
    }

    @Override
    public Action<FieldData> modifyField(ClassData classData, FieldData fieldData,
                                         @Nullable ClassMetadata classMetadata, @Nullable FieldMetadata fieldMetadata) {
        // Remove javadocs from synthetic fields
        if (fieldMetadata != null && fieldMetadata.hasAccessFlag(AccessFlag.SYNTHETIC) && !fieldData.getJavadoc().isEmpty()) {
            return Action.delete();
        }
        return Action.skip();
    }

    @Override
    public Action<MethodData> modifyMethod(ClassData classData, MethodData methodData,
                                           @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        // Remove javadocs from synthetic methods which aren't lambdas
        if (methodMetadata != null && methodMetadata.hasAccessFlag(AccessFlag.SYNTHETIC) && !methodMetadata.isLambda()) {
            return Action.delete();
        }
        return Action.skip();
    }
}
