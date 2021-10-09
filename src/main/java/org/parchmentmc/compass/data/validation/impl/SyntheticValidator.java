package org.parchmentmc.compass.data.validation.impl;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.data.validation.AbstractValidator;
import org.parchmentmc.compass.data.validation.ValidationIssue;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.FieldMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;
import org.parchmentmc.feather.util.AccessFlag;

import java.util.function.Consumer;

import static org.parchmentmc.feather.mapping.MappingDataContainer.*;

/**
 * Validates that synthetic fields, methods, and their parameters are not documented (or named, for parameters).
 */
public class SyntheticValidator extends AbstractValidator {
    public SyntheticValidator() {
        super("synthetic fields and methods");
    }

    @Override
    public void validate(Consumer<? super ValidationIssue> issueHandler, ClassData classData, FieldData fieldData,
                         @Nullable ClassMetadata classMetadata, @Nullable FieldMetadata fieldMetadata) {
        if (fieldMetadata != null && fieldMetadata.hasAccessFlag(AccessFlag.SYNTHETIC)) {
            if (!fieldData.getJavadoc().isEmpty()) {
                issueHandler.accept(error("Synthetic method must not be documented"));
            }
        }
    }

    @Override
    public void validate(Consumer<? super ValidationIssue> issueHandler, ClassData classData, MethodData methodData,
                         @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        if (methodMetadata != null && methodMetadata.hasAccessFlag(AccessFlag.SYNTHETIC) && !methodMetadata.isLambda()) {
            if (!methodData.getJavadoc().isEmpty()) {
                issueHandler.accept(error("Synthetic method must not be documented"));
            }
        }
    }

    @Override
    public void validate(Consumer<? super ValidationIssue> issueHandler, ClassData classData, MethodData methodData,
                         ParameterData paramData, @Nullable ClassMetadata classMetadata,
                         @Nullable MethodMetadata methodMetadata) {
        if (methodMetadata != null && methodMetadata.hasAccessFlag(AccessFlag.SYNTHETIC) && !methodMetadata.isLambda()) {
            if (paramData.getName() != null) {
                issueHandler.accept(error("Synthetic method parameter must not be named"));
            }
            if (paramData.getJavadoc() != null) {
                issueHandler.accept(error("Synthetic method parameter must not be documented"));
            }
        }
    }
}
