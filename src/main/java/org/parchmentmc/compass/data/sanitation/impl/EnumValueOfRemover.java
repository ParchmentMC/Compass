package org.parchmentmc.compass.data.sanitation.impl;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.data.sanitation.AbstractSanitizer;
import org.parchmentmc.feather.mapping.MappingDataContainer;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;
import org.parchmentmc.feather.util.AccessFlag;

public class EnumValueOfRemover extends AbstractSanitizer {
    public EnumValueOfRemover() {
        super("enum valueOf remover");
    }

    @Override
    public boolean start(boolean isMetadataAvailable) {
        return isMetadataAvailable;
    }

    @Override
    public Action<MappingDataContainer.MethodData> sanitize(MappingDataContainer.ClassData classData, MappingDataContainer.MethodData methodData, @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        if (classMetadata != null && classMetadata.hasAccessFlag(AccessFlag.ENUM)
                && methodData.getName().equals("valueOf")
                && methodData.getDescriptor().equals("(Ljava/lang/String;)L" + classData.getName() + ';')) {
            return Action.delete();
        }
        return super.sanitize(classData, methodData, classMetadata, methodMetadata);
    }
}
