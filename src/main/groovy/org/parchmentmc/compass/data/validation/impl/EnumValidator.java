package org.parchmentmc.compass.data.validation.impl;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.data.validation.Validator;
import org.parchmentmc.feather.mapping.MappingDataContainer.ClassData;
import org.parchmentmc.feather.mapping.MappingDataContainer.FieldData;
import org.parchmentmc.feather.mapping.MappingDataContainer.MethodData;
import org.parchmentmc.feather.mapping.MappingDataContainer.ParameterData;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.FieldMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;
import org.parchmentmc.feather.util.AccessFlag;

/**
 * Validates that the <code>{@value #VALUES_FIELD_NAME}</code> field of {@link Enum enum classes} is not documented.
 */
public class EnumValidator extends Validator {
    public static final String VALUES_FIELD_NAME = "$VALUES";
    public static final String VALUE_OF_METHOD_NAME = "valueOf";
    public static final String VALUE_OF_METHOD_DESCRIPTOR_FORMAT = "(Ljava/lang/String;)L%s;";

    public EnumValidator() {
        super("enum " + VALUES_FIELD_NAME + " and " + VALUE_OF_METHOD_NAME);
    }

    @Override
    public boolean preVisit(DataType type) {
        return DataType.FIELDS.test(type) || DataType.METHODS.test(type) || DataType.PARAMETERS.test(type);
    }

    @Override
    public void visitField(ClassData classData, FieldData fieldData,
                           @Nullable ClassMetadata classMetadata, @Nullable FieldMetadata fieldMetadata) {
        if (classMetadata != null && classMetadata.hasAccessFlag(AccessFlag.ENUM)) {
            if (fieldData.getName().equals(VALUES_FIELD_NAME) && !fieldData.getJavadoc().isEmpty()) {
                error(VALUES_FIELD_NAME + " field of enum class should not be documented");
            }
        }
    }

    @Override
    public boolean visitMethod(ClassData classData, MethodData methodData,
                               @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        if (classMetadata != null && classMetadata.hasAccessFlag(AccessFlag.ENUM)) {
            if (methodData.getName().equals(VALUE_OF_METHOD_NAME)
                && methodData.getDescriptor().equals(String.format(VALUE_OF_METHOD_DESCRIPTOR_FORMAT, classData.getName()))
                && (methodMetadata == null || methodMetadata.isStatic())
                && !methodData.getJavadoc().isEmpty()) {
                error(VALUE_OF_METHOD_NAME + " method should not be documented");
            }
        }
        return true;
    }

    @Override
    public void visitParameter(ClassData classData, MethodData methodData, ParameterData paramData,
                               @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        if (classMetadata != null && classMetadata.hasAccessFlag(AccessFlag.ENUM)) {
            if (methodData.getName().equals(VALUE_OF_METHOD_NAME)
                && methodData.getDescriptor().equals(String.format(VALUE_OF_METHOD_DESCRIPTOR_FORMAT, classData.getName()))
                && (methodMetadata == null || methodMetadata.isStatic())) {
                if (paramData.getName() != null) {
                    error("parameter of " + VALUE_OF_METHOD_NAME + " method should not be named");
                }
                if (paramData.getJavadoc() != null) {
                    error("parameter of " + VALUE_OF_METHOD_NAME + " method should not be documented");
                }
            }
        }
    }
}
