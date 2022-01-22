package org.parchmentmc.compass.data.validation.impl;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.data.validation.Validator;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.FieldMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;
import org.parchmentmc.feather.util.AccessFlag;

import static org.parchmentmc.feather.mapping.MappingDataContainer.ClassData;
import static org.parchmentmc.feather.mapping.MappingDataContainer.FieldData;
import static org.parchmentmc.feather.mapping.MappingDataContainer.MethodData;
import static org.parchmentmc.feather.mapping.MappingDataContainer.ParameterData;

/**
 * Validates that synthetic fields, methods, and their parameters are not documented (or named, for parameters).
 */
public class SyntheticValidator extends Validator {
    public SyntheticValidator() {
        super("synthetic fields and methods");
    }

    @Override
    public boolean preVisit(DataType type) {
        return DataType.FIELDS.test(type) || DataType.METHODS.test(type) || DataType.PARAMETERS.test(type);
    }

    @Override
    public void visitField(ClassData classData, FieldData fieldData,
                           @Nullable ClassMetadata classMetadata, @Nullable FieldMetadata fieldMetadata) {
        if (fieldMetadata != null && fieldMetadata.hasAccessFlag(AccessFlag.SYNTHETIC)) {
            if (!fieldData.getJavadoc().isEmpty()) {
                error("Synthetic method must not be documented");
            }
        }
    }

    @Override
    public boolean visitMethod(ClassData classData, MethodData methodData,
                               @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        if (methodMetadata != null && methodMetadata.hasAccessFlag(AccessFlag.SYNTHETIC) && !methodMetadata.isLambda()) {
            if (!methodData.getJavadoc().isEmpty()) {
                error("Synthetic method must not be documented");
            }
        }
        return true;
    }

    @Override
    public void visitParameter(ClassData classData, MethodData methodData, ParameterData paramData,
                               @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        if (methodMetadata != null && methodMetadata.hasAccessFlag(AccessFlag.SYNTHETIC) && !methodMetadata.isLambda()) {
            if (paramData.getName() != null) {
                error("Synthetic method parameter must not be named");
            }
            if (paramData.getJavadoc() != null) {
                error("Synthetic method parameter must not be documented");
            }
        }
    }
}
