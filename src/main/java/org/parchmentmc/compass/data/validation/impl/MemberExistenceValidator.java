package org.parchmentmc.compass.data.validation.impl;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.data.validation.Validator;
import org.parchmentmc.compass.util.DescriptorIndexer;
import org.parchmentmc.feather.mapping.MappingDataContainer;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.FieldMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;
import org.parchmentmc.feather.metadata.SourceMetadata;

import java.util.BitSet;

/**
 * Validates that classes, fields, methods, and parameters exist according to the Blackstone metadata.
 *
 * @see DescriptorIndexer
 */
public class MemberExistenceValidator extends Validator {
    @MonotonicNonNull
    private DescriptorIndexer indexer;

    public MemberExistenceValidator() {
        super("member existence");
    }

    @Override
    public boolean visit(MappingDataContainer container, @Nullable SourceMetadata metadata) {
        indexer = new DescriptorIndexer();
        return metadata != null; // Only visit when we have metadata available
    }

    @Override
    public boolean preVisit(DataType type) {
        return DataType.CLASSES.test(type)
                || DataType.FIELDS.test(type)
                || DataType.METHODS.test(type)
                || DataType.PARAMETERS.test(type);
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
            error("Method does not exist according to metadata");
            return false;
        }
        return true;
    }

    @Override
    public void visitParameter(MappingDataContainer.ClassData classData, MappingDataContainer.MethodData methodData,
                               MappingDataContainer.ParameterData paramData, @Nullable ClassMetadata classMetadata,
                               @Nullable MethodMetadata methodMetadata) {
        final BitSet indexes = indexer.getIndexes(methodData, methodMetadata);
        byte paramIndex = paramData.getIndex();

        if (!indexes.get(paramIndex)) {
            // When above is resolved, add special-case for index 0 and non-static method
            error("Parameter does not exist according to descriptor");
        }
    }
}
