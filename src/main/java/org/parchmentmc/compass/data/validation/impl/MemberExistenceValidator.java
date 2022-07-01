package org.parchmentmc.compass.data.validation.impl;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.data.validation.Validator;
import org.parchmentmc.feather.mapping.MappingDataContainer;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.FieldMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;
import org.parchmentmc.feather.metadata.SourceMetadata;

/**
 * Validates that classes, fields, and methods exist according to the Blackstone metadata.
 */
public class MemberExistenceValidator extends Validator {
    public MemberExistenceValidator() {
        super("member existence");
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
    public boolean visitClass(MappingDataContainer.ClassData classData, @Nullable ClassMetadata classMetadata) {
        if (classMetadata == null) {
            error("Class does not exist according to metadata");
            return false;
        }
        return true;
    }

    @Override
    public void visitField(MappingDataContainer.ClassData classData, MappingDataContainer.FieldData fieldData, 
                           @Nullable ClassMetadata classMetadata, @Nullable FieldMetadata fieldMetadata) {
        if (fieldMetadata == null) {
            error("Field does not exist according to metadata");
        }
    }

    @Override
    public boolean visitMethod(MappingDataContainer.ClassData classData, MappingDataContainer.MethodData methodData, 
                               @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        if (methodMetadata == null) {
            error("Class does not exist according to metadata");
        }
        return false;
    }
}
