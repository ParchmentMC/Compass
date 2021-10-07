package org.parchmentmc.compass.data.validation.impl;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.data.validation.AbstractValidator;
import org.parchmentmc.compass.data.validation.ValidationIssue;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;
import org.parchmentmc.feather.util.AccessFlag;

import java.util.function.Consumer;

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
    public void validate(Consumer<? super ValidationIssue> issues, ClassData classData, MethodData methodData,
                         @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        if (methodData.getName().startsWith(BRIDGE_METHOD_NAME_PREFIX)
                && (methodMetadata == null || methodMetadata.hasAccessFlag(AccessFlag.BRIDGE))) {
            if (methodData.getJavadoc().isEmpty()) {
                issues.accept(error("Bridge method must not be documented"));
            }
        }
    }

    @Override
    public void validate(Consumer<? super ValidationIssue> issues, ClassData classData, MethodData methodData,
                         ParameterData paramData, @Nullable ClassMetadata classMetadata,
                         @Nullable MethodMetadata methodMetadata) {

        if (methodData.getName().startsWith(BRIDGE_METHOD_NAME_PREFIX)
                && (methodMetadata == null || methodMetadata.hasAccessFlag(AccessFlag.BRIDGE))) {
            if (paramData.getName() != null) {
                issues.accept(error("Bridge method parameter must not be named"));
            }
            if (paramData.getJavadoc() != null) {
                issues.accept(error("Bridge method parameter must not be documented"));
            }
        }
    }
}
