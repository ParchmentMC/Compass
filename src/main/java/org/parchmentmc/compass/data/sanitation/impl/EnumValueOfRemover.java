package org.parchmentmc.compass.data.sanitation.impl;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.data.sanitation.Sanitizer;
import org.parchmentmc.feather.mapping.MappingDataContainer;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;
import org.parchmentmc.feather.metadata.SourceMetadata;
import org.parchmentmc.feather.util.AccessFlag;

import static org.parchmentmc.feather.mapping.MappingDataContainer.ClassData;
import static org.parchmentmc.feather.mapping.MappingDataContainer.MethodData;

public class EnumValueOfRemover extends Sanitizer {
    public EnumValueOfRemover() {
        super("enum valueOf remover");
    }

    @Override
    public boolean visit(MappingDataContainer container, @Nullable SourceMetadata metadata) {
        return metadata != null; // Skip if metadata is not available
    }

    @Override
    public Action<MethodData> modifyMethod(ClassData classData, MethodData methodData,
                                           @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        if (classMetadata != null && classMetadata.hasAccessFlag(AccessFlag.ENUM)
                && methodData.getName().equals("valueOf")
                && methodData.getDescriptor().equals("(Ljava/lang/String;)L" + classData.getName() + ';')) {
            return Action.delete();
        }
        return Action.skip();
    }
}
