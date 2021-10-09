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
    /**
     * Validates the given package data.
     *
     * @param issueHandler the issues handler for the package data
     * @param packageData  the package data to be validated
     */
    default void validate(Consumer<? super ValidationIssue> issueHandler, PackageData packageData) {
    }

    /**
     * Validates the given class data.
     *
     * @param issueHandler  the issues handler for the class data
     * @param classData     the class data to be validated
     * @param classMetadata the class metadata, may be {@code null}
     */
    default void validate(Consumer<? super ValidationIssue> issueHandler, ClassData classData,
                          @Nullable ClassMetadata classMetadata) {
    }

    /**
     * Validates the given field data.
     *
     * @param issueHandler  the issues handler for the field data
     * @param classData     the owning class data
     * @param fieldData     the field data to be validated
     * @param classMetadata the class metadata, may be {@code null}
     * @param fieldMetadata the field metadata, may be {@code null}
     */
    default void validate(Consumer<? super ValidationIssue> issueHandler, ClassData classData, FieldData fieldData,
                          @Nullable ClassMetadata classMetadata, @Nullable FieldMetadata fieldMetadata) {
    }

    /**
     * Validates the given method data.
     *
     * @param issueHandler   the issues handler for the field data
     * @param classData      the owning class data
     * @param methodData     the method data to be validated
     * @param classMetadata  the class metadata, may be {@code null}
     * @param methodMetadata the method metadata, may be {@code null}
     */
    default void validate(Consumer<? super ValidationIssue> issueHandler, ClassData classData, MethodData methodData,
                          @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
    }

    /**
     * Validates the given parameter data.
     *
     * @param issueHandler   the issues handler for the field data
     * @param classData      the owning class data
     * @param methodData     the owning method data
     * @param paramData      the parameter data to be validated
     * @param classMetadata  the class metadata, may be {@code null}
     * @param methodMetadata the method metadata, may be {@code null}
     */
    default void validate(Consumer<? super ValidationIssue> issueHandler, ClassData classData, MethodData methodData,
                          ParameterData paramData, @Nullable ClassMetadata classMetadata,
                          @Nullable MethodMetadata methodMetadata) {
    }
}
