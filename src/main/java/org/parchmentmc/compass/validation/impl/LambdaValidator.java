package org.parchmentmc.compass.validation.impl;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.validation.AbstractValidator;
import org.parchmentmc.compass.validation.ValidationIssue;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.parchmentmc.feather.mapping.MappingDataContainer.*;

/**
 * Validates that neither lambda methods nor their parameters are documented.
 */
public class LambdaValidator extends AbstractValidator {
    private static final String LAMBDA_METHOD_NAME_PREFIX = "lambda$";

    public LambdaValidator() {
        super("lambda methods");
    }

    @Override
    public List<? extends ValidationIssue> validate(ClassData classData, MethodData methodData,
                                                    @Nullable ClassMetadata classMetadata,
                                                    @Nullable MethodMetadata methodMetadata) {
        if (methodData.getName().startsWith(LAMBDA_METHOD_NAME_PREFIX)
                && (methodMetadata == null || methodMetadata.isLambda())) {
            if (!methodData.getJavadoc().isEmpty()) {
                return singletonList(error("Lambda method must not be documented"));
            }
        }

        return emptyList();
    }

    @Override
    public List<? extends ValidationIssue> validate(ClassData classData, MethodData methodData, ParameterData paramData,
                                                    @Nullable ClassMetadata classMetadata,
                                                    @Nullable MethodMetadata methodMetadata) {
        if (methodData.getName().startsWith(LAMBDA_METHOD_NAME_PREFIX)
                && (methodMetadata == null || methodMetadata.isLambda())) {
            if (paramData.getJavadoc() != null) {
                return singletonList(error("Lambda method parameter must not be documented"));
            }
        }

        return emptyList();
    }
}
