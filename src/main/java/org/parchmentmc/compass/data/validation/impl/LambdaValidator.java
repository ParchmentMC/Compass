package org.parchmentmc.compass.data.validation.impl;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.data.validation.AbstractValidator;
import org.parchmentmc.compass.data.validation.ValidationIssue;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;

import java.util.function.Consumer;

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
    public void validate(Consumer<? super ValidationIssue> issueHandler, ClassData classData, MethodData methodData,
                         @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        if (methodData.getName().startsWith(LAMBDA_METHOD_NAME_PREFIX)
                && (methodMetadata == null || methodMetadata.isLambda())) {
            if (!methodData.getJavadoc().isEmpty()) {
                issueHandler.accept(error("Lambda method must not be documented"));
            }
        }
    }

    @Override
    public void validate(Consumer<? super ValidationIssue> issueHandler, ClassData classData, MethodData methodData,
                         ParameterData paramData, @Nullable ClassMetadata classMetadata,
                         @Nullable MethodMetadata methodMetadata) {
        if (methodData.getName().startsWith(LAMBDA_METHOD_NAME_PREFIX)
                && (methodMetadata == null || methodMetadata.isLambda())) {
            if (paramData.getJavadoc() != null) {
                issueHandler.accept(error("Lambda method parameter must not be documented"));
            }
        }
    }
}
