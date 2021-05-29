package org.parchmentmc.compass.validation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.gradle.api.Named;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.FieldMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;

import java.util.Collections;
import java.util.List;

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
    default List<? extends ValidationIssue> validate(PackageData packageData) {
        return Collections.emptyList();
    }

    default List<? extends ValidationIssue> validate(ClassData classData,
                                                     @Nullable ClassMetadata classMetadata) {
        return Collections.emptyList();
    }

    default List<? extends ValidationIssue> validate(ClassData classData, FieldData fieldData,
                                                     @Nullable ClassMetadata classMetadata,
                                                     @Nullable FieldMetadata fieldMetadata) {
        return Collections.emptyList();
    }

    default List<? extends ValidationIssue> validate(ClassData classData, MethodData methodData,
                                                     @Nullable ClassMetadata classMetadata,
                                                     @Nullable MethodMetadata methodMetadata) {
        return Collections.emptyList();
    }

    default List<? extends ValidationIssue> validate(ClassData classData, MethodData methodData, ParameterData paramData,
                                                     @Nullable ClassMetadata classMetadata,
                                                     @Nullable MethodMetadata methodMetadata) {
        return Collections.emptyList();
    }
}
