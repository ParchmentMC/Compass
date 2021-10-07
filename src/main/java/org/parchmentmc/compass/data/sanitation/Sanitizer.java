package org.parchmentmc.compass.data.sanitation;

import com.google.common.base.Preconditions;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.gradle.api.Named;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.FieldMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;

import static org.parchmentmc.feather.mapping.MappingDataBuilder.*;

/**
 * A sanitizer for mapping data.
 *
 * <p>The sanitize methods within this class may not return {@code null}. Instead, they must return an appropriate
 * action such as {@link Action#nothing()}</p>
 *
 * <p>The mapping data passed into the validators must be in Official names.</p>
 *
 * <p>Implementors should override one of the {@code sanitize} methods and implement their validation logic, and use
 * the provided {@link AbstractSanitizer} to ease implementation.</p>
 */
public interface Sanitizer extends Named {
    /**
     * Called before this sanitizer is passed over the mapping data. The return value indicates whether to continue
     * with the sanitizer's pass, where {@code false} means the sanitizer will be skipped as completed.
     *
     * <p>This is called at least once when the sanitizer is first ran, and subsequently for each revisit (as requested
     * by {@link #revisit()}.</p>
     *
     * @param isMetadataAvailable whether the {@link org.parchmentmc.feather.metadata.SourceMetadata} is available
     * @return whether to start
     */
    default boolean start(boolean isMetadataAvailable) {
        return true;
    }

    /**
     * Sanitizes the given package data.
     *
     * <p>A {@link Action#delete() deletion action} will only affect the package information; any classes which are
     * within the package will not be affected.</p>
     *
     * @param packageData the package data to be sanitized
     * @return an action to take on the package data
     */
    default Action<PackageData> sanitize(PackageData packageData) {
        return Action.nothing();
    }

    /**
     * Sanitizes the given class data.
     *
     * @param classData     the class data to be sanitized
     * @param classMetadata the class metadata, may be {@code null}
     * @return an action to take on the class data
     */
    default Action<ClassData> sanitize(ClassData classData,
                                       @Nullable ClassMetadata classMetadata) {
        return Action.nothing();
    }

    /**
     * Sanitizes the given field data.
     *
     * @param classData     the owning class data
     * @param fieldData     the field data to be sanitized
     * @param classMetadata the class metadata, may be {@code null}
     * @param fieldMetadata the field metadata, may be {@code null}
     * @return an action to take on the field data
     */
    default Action<FieldData> sanitize(ClassData classData, FieldData fieldData,
                                       @Nullable ClassMetadata classMetadata, @Nullable FieldMetadata fieldMetadata) {
        return Action.nothing();
    }

    /**
     * Sanitizes the given method data.
     *
     * @param classData      the owning class data
     * @param methodData     the method data to be sanitized
     * @param classMetadata  the class metadata, may be {@code null}
     * @param methodMetadata the method metadata, may be {@code null}
     * @return an action to take on the method data
     */
    default Action<MethodData> sanitize(ClassData classData, MethodData methodData,
                                        @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        return Action.nothing();
    }

    /**
     * Sanitizes the given parameter data.
     *
     * @param classData      the owning class data
     * @param methodData     the owning method data
     * @param paramData      the parameter data to be sanitized
     * @param classMetadata  the class metadata, may be {@code null}
     * @param methodMetadata the method metadata, may be {@code null}
     * @return an action to take on the parameter data
     */
    default Action<ParameterData> sanitize(ClassData classData, MethodData methodData, ParameterData paramData,
                                           @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        return Action.nothing();
    }

    /**
     * Returns if the sanitizer should re-visit the mapping data. This allows a sanitizer to do multiple passes over
     * the mapping data, such as for collecting some required information about the mapping data as a whole before
     * doing any modifications.
     *
     * @return whether to revisit the mapping data.
     */
    default boolean revisit() {
        return false;
    }

    /**
     * An action to take on the target element.
     *
     * @param <T> the type of the target element
     */
    final class Action<T> {
        /**
         * Returns an action which does nothing and continues visiting children elements of the target element.
         *
         * @param <T> the type of the target element
         * @return an action which does nothing and continues visiting children elements
         */
        @SuppressWarnings("unchecked")
        public static <T> Action<T> nothing() {
            return (Action<T>) NOTHING;
        }

        /**
         * Returns an action which does nothing and skips visiting children elements of the target element.
         *
         * <p>Using this action against an element which contains no children elements has no effect.</p>
         *
         * @param <T> the type of the target element
         * @return an action which does nothing and skips visiting children elements
         */
        @SuppressWarnings("unchecked")
        public static <T> Action<T> skip() {
            return (Action<T>) SKIP;
        }

        /**
         * Returns an action which modifies the target element and may skip visiting its children elements.
         *
         * @param newData the new data for the target element
         * @param skip    whether to skip visiting children elements of the target
         * @param <T>     the type of the target element
         * @return an action which modifies the target element and may skip visiting children elements
         */
        public static <T> Action<T> modify(T newData, boolean skip) {
            Preconditions.checkNotNull(newData, "New data must not be null");
            return new Action<>(ActionType.MODIFY, skip, newData);
        }

        /**
         * Returns an action which deletes the target element and any data associated with it, including children
         * elements.
         *
         * @param <T> the type of the target element
         * @return an action which deletes the target element, including children
         */
        @SuppressWarnings("unchecked")
        public static <T> Action<T> delete() {
            return (Action<T>) DELETE;
        }

        // Reusable constants instead of constructing new objects for each one
        private static final Action<?> NOTHING = new Action<>(ActionType.NOTHING, false, null);
        private static final Action<?> SKIP = new Action<>(ActionType.NOTHING, true, null);
        private static final Action<?> DELETE = new Action<>(ActionType.DELETE, true, null);

        final ActionType type;
        final boolean skip;
        @Nullable
        final T data;

        private Action(ActionType type, boolean skip, @Nullable T data) {
            this.type = type;
            this.skip = skip;
            this.data = data;
        }

        enum ActionType {
            NOTHING,
            MODIFY,
            DELETE
        }
    }
}
