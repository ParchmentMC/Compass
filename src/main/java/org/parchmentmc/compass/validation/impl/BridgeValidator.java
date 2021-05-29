package org.parchmentmc.compass.validation.impl;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.validation.AbstractValidator;
import org.parchmentmc.compass.validation.ValidationIssue;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;
import org.parchmentmc.feather.util.AccessFlag;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.parchmentmc.feather.mapping.MappingDataContainer.*;

/**
 * Validates that neither bridge methods are not documented, nor are their parameters named or documented.
 */
public class BridgeValidator extends AbstractValidator {
    private static final String BRIDGE_METHOD_NAME_PREFIX = "access$";

    public BridgeValidator() {
        super("bridge methods");
    }

    @Override
    public List<? extends ValidationIssue> validate(ClassData classData, MethodData methodData,
                                                    @Nullable ClassMetadata classMetadata,
                                                    @Nullable MethodMetadata methodMetadata) {
        if (methodData.getName().startsWith(BRIDGE_METHOD_NAME_PREFIX)
                && (methodMetadata == null || methodMetadata.hasAccessFlag(AccessFlag.BRIDGE))) {
            if (methodData.getJavadoc().isEmpty()) {
                return singletonList(error("Bridge method must not be documented"));
            }
        }

        return emptyList();
    }

    @Override
    public List<? extends ValidationIssue> validate(ClassData classData, MethodData methodData, ParameterData paramData,
                                                    @Nullable ClassMetadata classMetadata,
                                                    @Nullable MethodMetadata methodMetadata) {

        if (methodData.getName().startsWith(BRIDGE_METHOD_NAME_PREFIX)
                && (methodMetadata == null || methodMetadata.hasAccessFlag(AccessFlag.BRIDGE))) {
            List<ValidationIssue.ValidationError> errors = new ArrayList<>();
            if (paramData.getName() != null) {
                errors.add(error("Bridge method parameter must not be named"));
            }
            if (paramData.getJavadoc() != null) {
                errors.add(error("Bridge method parameter must not be documented"));
            }
            return errors;
        }

        return emptyList();
    }
}
