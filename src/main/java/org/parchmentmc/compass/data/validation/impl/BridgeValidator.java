package org.parchmentmc.compass.data.validation.impl;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.data.validation.Validator;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;
import org.parchmentmc.feather.util.AccessFlag;

import static org.parchmentmc.feather.mapping.MappingDataContainer.ClassData;
import static org.parchmentmc.feather.mapping.MappingDataContainer.MethodData;
import static org.parchmentmc.feather.mapping.MappingDataContainer.ParameterData;

/**
 * Validates that neither bridge methods are not documented, nor are their parameters named or documented.
 */
public class BridgeValidator extends Validator {
    private static final String BRIDGE_METHOD_NAME_PREFIX = "access$";

    public BridgeValidator() {
        super("bridge methods");
    }

    @Override
    public boolean preVisit(DataType type) {
        return DataType.METHODS.test(type) || DataType.PARAMETERS.test(type);
    }

    @Override
    public boolean visitMethod(ClassData classData, MethodData methodData,
                               @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        if (methodData.getName().startsWith(BRIDGE_METHOD_NAME_PREFIX)
            && (methodMetadata == null || methodMetadata.hasAccessFlag(AccessFlag.BRIDGE))) {
            if (methodData.getJavadoc().isEmpty()) {
                error("Bridge method must not be documented");
            }
        }
        return true;
    }

    @Override
    public void visitParameter(ClassData classData, MethodData methodData, ParameterData paramData,
                               @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        if (methodData.getName().startsWith(BRIDGE_METHOD_NAME_PREFIX)
            && (methodMetadata == null || methodMetadata.hasAccessFlag(AccessFlag.BRIDGE))) {
            if (paramData.getName() != null) {
                error("Bridge method parameter must not be named");
            }
            if (paramData.getJavadoc() != null) {
                error("Bridge method parameter must not be documented");
            }
        }
    }
}
