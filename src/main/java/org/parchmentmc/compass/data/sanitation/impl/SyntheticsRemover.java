package org.parchmentmc.compass.data.sanitation.impl;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.data.sanitation.AbstractSanitizer;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.FieldMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;
import org.parchmentmc.feather.util.AccessFlag;

import static org.parchmentmc.feather.mapping.MappingDataContainer.*;

/**
 * Removes synthetics fields and (non-lambda) methods.
 */
public class SyntheticsRemover extends AbstractSanitizer {
    public SyntheticsRemover() {
        super("synthetics");
    }

    @Override
    public boolean start(boolean isMetadataAvailable) {
        return isMetadataAvailable;
    }

    @Override
    public Action<FieldData> sanitize(ClassData classData, FieldData fieldData,
                                      @Nullable ClassMetadata classMetadata, @Nullable FieldMetadata fieldMetadata) {
        // Remove javadocs from synthetic fields
        if (fieldMetadata != null && fieldMetadata.hasAccessFlag(AccessFlag.SYNTHETIC) && !fieldData.getJavadoc().isEmpty()) {
            return Action.delete();
        }
        return super.sanitize(classData, fieldData, classMetadata, fieldMetadata);
    }

    @Override
    public Action<MethodData> sanitize(ClassData classData, MethodData methodData,
                                       @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        // Remove javadocs from synthetic methods which aren't lambdas
        if (methodMetadata != null && methodMetadata.hasAccessFlag(AccessFlag.SYNTHETIC) && !methodMetadata.isLambda()) {
            return Action.delete();
        }
        return super.sanitize(classData, methodData, classMetadata, methodMetadata);
    }
}
