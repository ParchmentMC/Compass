package org.parchmentmc.compass.data.validation.impl;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.data.validation.Validator;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;

import static org.parchmentmc.feather.mapping.MappingDataContainer.ClassData;
import static org.parchmentmc.feather.mapping.MappingDataContainer.MethodData;
import static org.parchmentmc.feather.mapping.MappingDataContainer.ParameterData;

/**
 * Validates that neither lambda methods nor their parameters are documented.
 */
public class LambdaValidator extends Validator {
    private static final String LAMBDA_METHOD_NAME_PREFIX = "lambda$";

    public LambdaValidator() {
        super("lambda methods");
    }

    @Override
    public boolean visitMethod(ClassData classData, MethodData methodData,
                               @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        if (methodData.getName().startsWith(LAMBDA_METHOD_NAME_PREFIX)
            && (methodMetadata == null || methodMetadata.isLambda())) {
            if (!methodData.getJavadoc().isEmpty()) {
                error("Lambda method must not be documented");
            }
        }
        return true;
    }

    @Override
    public void visitParameter(ClassData classData, MethodData methodData, ParameterData paramData,
                               @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        if (methodData.getName().startsWith(LAMBDA_METHOD_NAME_PREFIX)
            && (methodMetadata == null || methodMetadata.isLambda())) {
            if (paramData.getJavadoc() != null) {
                error("Lambda method parameter must not be documented");
            }
        }
    }
}
