package org.parchmentmc.compass.validation.impl;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.validation.AbstractValidator;
import org.parchmentmc.compass.validation.ValidationIssue;
import org.parchmentmc.feather.mapping.MappingDataContainer.ClassData;
import org.parchmentmc.feather.mapping.MappingDataContainer.FieldData;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.FieldMetadata;
import org.parchmentmc.feather.util.AccessFlag;

import java.util.function.Consumer;

/**
 * Validates that the <code>{@value #VALUES_FIELD_NAME}</code> field of {@link Enum enum classes} is not documented.
 */
public class EnumValuesValidator extends AbstractValidator {
    public static final String VALUES_FIELD_NAME = "$VALUES";

    public EnumValuesValidator() {
        super("enum " + VALUES_FIELD_NAME);
    }

    @Override
    public void validate(Consumer<? super ValidationIssue> issues, ClassData classData, FieldData fieldData,
                         @Nullable ClassMetadata classMetadata, @Nullable FieldMetadata fieldMetadata) {
        if (classMetadata != null && classMetadata.hasAccessFlag(AccessFlag.ENUM)) {
            if (fieldData.getName().equals(VALUES_FIELD_NAME) && !fieldData.getJavadoc().isEmpty()) {
                issues.accept(error(VALUES_FIELD_NAME + " field of enum class should not be documented"));
            }
        }
    }
}
