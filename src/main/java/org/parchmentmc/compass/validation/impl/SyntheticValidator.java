package org.parchmentmc.compass.validation.impl;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.validation.AbstractValidator;
import org.parchmentmc.compass.validation.ValidationIssue;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.FieldMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;
import org.parchmentmc.feather.util.AccessFlag;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.parchmentmc.feather.mapping.MappingDataContainer.*;

/**
 * Validates that synthetic fields, methods, and their parameters are not documented (or named, for parameters).
 */
public class SyntheticValidator extends AbstractValidator {
    public SyntheticValidator() {
        super("synthetic fields and methods");
    }

    @Override
    public List<? extends ValidationIssue> validate(ClassData classData, FieldData fieldData, @Nullable ClassMetadata classMetadata, @Nullable FieldMetadata fieldMetadata) {
        if (fieldMetadata != null && fieldMetadata.hasAccessFlag(AccessFlag.SYNTHETIC)) {
            if (!fieldData.getJavadoc().isEmpty()) {
                return singletonList(error("Synthetic method must not be documented"));
            }
        }

        return emptyList();
    }

    @Override
    public List<? extends ValidationIssue> validate(ClassData classData, MethodData methodData,
                                                    @Nullable ClassMetadata classMetadata,
                                                    @Nullable MethodMetadata methodMetadata) {
        if (methodMetadata != null && methodMetadata.hasAccessFlag(AccessFlag.SYNTHETIC) && !methodMetadata.isLambda()) {
            if (!methodData.getJavadoc().isEmpty()) {
                return singletonList(error("Synthetic method must not be documented"));
            }
        }

        return emptyList();
    }

    @Override
    public List<? extends ValidationIssue> validate(ClassData classData, MethodData methodData, ParameterData paramData,
                                                    @Nullable ClassMetadata classMetadata,
                                                    @Nullable MethodMetadata methodMetadata) {
        if (methodMetadata != null && methodMetadata.hasAccessFlag(AccessFlag.SYNTHETIC) && !methodMetadata.isLambda()) {
            List<ValidationIssue.ValidationError> errors = new ArrayList<>();
            if (paramData.getName() != null) {
                errors.add(error("Synthetic method parameter must not be named"));
            }
            if (paramData.getJavadoc() != null) {
                errors.add(error("Synthetic method parameter must not be documented"));
            }
            return errors;
        }

        return emptyList();
    }
}
