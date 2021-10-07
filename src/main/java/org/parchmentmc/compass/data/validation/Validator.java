package org.parchmentmc.compass.data.validation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.gradle.api.Named;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.FieldMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;

import java.util.function.Consumer;

import static org.parchmentmc.feather.mapping.MappingDataContainer.*;

/**
 * A validator for mapping data.
 *
 * <p>The mapping data passed into the validators must be in Official names.</p>
 *
 * <p>Implementors should override one of the {@code validate} methods and implement their validation logic, and use
 * the provided {@link AbstractValidator} to ease implementation.</p>
 *
 * @see org.parchmentmc.feather.mapping.MappingDataContainer
 */
public interface Validator extends Named {
    default void validate(Consumer<? super ValidationIssue> issues, PackageData packageData) {
    }

    default void validate(Consumer<? super ValidationIssue> issues, ClassData classData,
                          @Nullable ClassMetadata classMetadata) {
    }

    default void validate(Consumer<? super ValidationIssue> issues, ClassData classData, FieldData fieldData,
                          @Nullable ClassMetadata classMetadata, @Nullable FieldMetadata fieldMetadata) {
    }

    default void validate(Consumer<? super ValidationIssue> issues, ClassData classData, MethodData methodData,
                          @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
    }

    default void validate(Consumer<? super ValidationIssue> issues, ClassData classData, MethodData methodData,
                          ParameterData paramData, @Nullable ClassMetadata classMetadata,
                          @Nullable MethodMetadata methodMetadata) {
    }
}
