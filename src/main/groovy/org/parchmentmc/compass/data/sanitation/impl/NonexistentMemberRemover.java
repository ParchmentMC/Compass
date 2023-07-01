package org.parchmentmc.compass.data.sanitation.impl;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.data.sanitation.Sanitizer;
import org.parchmentmc.feather.mapping.MappingDataContainer;
import org.parchmentmc.feather.mapping.MappingDataContainer.ClassData;
import org.parchmentmc.feather.mapping.MappingDataContainer.FieldData;
import org.parchmentmc.feather.mapping.MappingDataContainer.MethodData;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.FieldMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;
import org.parchmentmc.feather.metadata.SourceMetadata;

public class NonexistentMemberRemover extends Sanitizer {
    public NonexistentMemberRemover() {
        super("non-existing members");
    }

    @Override
    public boolean visit(MappingDataContainer container, @Nullable SourceMetadata metadata) {
        return metadata != null; // Only visit when we have metadata available
    }

    @Override
    public boolean preVisit(DataType type) {
        return DataType.CLASSES.test(type) || DataType.FIELDS.test(type) || DataType.METHODS.test(type);
    }

    @Override
    public Action<ClassData> modifyClass(ClassData classData, @Nullable ClassMetadata classMetadata) {
        return classMetadata == null ? Action.delete() : Action.nothing();
    }

    @Override
    public Action<FieldData> modifyField(ClassData classData, FieldData fieldData,
                                         @Nullable ClassMetadata classMetadata, @Nullable FieldMetadata fieldMetadata) {
        return fieldMetadata == null ? Action.delete() : Action.nothing();
    }

    @Override
    public Action<MethodData> modifyMethod(ClassData classData, MethodData methodData,
                                           @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        return methodMetadata == null ? Action.delete() : Action.nothing();
    }
}
