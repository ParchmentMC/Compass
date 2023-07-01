package org.parchmentmc.compass.data.visitation;

import com.google.common.base.Preconditions;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.feather.mapping.MappingDataBuilder;
import org.parchmentmc.feather.mapping.MappingDataContainer;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.FieldMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;
import org.parchmentmc.feather.metadata.SourceMetadata;

import static org.parchmentmc.feather.mapping.MappingDataContainer.ClassData;
import static org.parchmentmc.feather.mapping.MappingDataContainer.FieldData;
import static org.parchmentmc.feather.mapping.MappingDataContainer.MethodData;
import static org.parchmentmc.feather.mapping.MappingDataContainer.PackageData;
import static org.parchmentmc.feather.mapping.MappingDataContainer.ParameterData;

/**
 * A {@link DataVisitor} which may modify or delete mapping data while visiting.
 *
 * <p>The {@code modify*} methods within this class may not return {@code null}. Instead, they must return an
 * appropriate action such as {@link Action#nothing()}</p>
 *
 * <p>If {@link DataVisitor#visit(int, DataVisitor, MappingDataContainer, SourceMetadata)} is called with a visitor of
 * this type, then this visitor will effectively have <strong>read-only</strong> access to the mapping data; any
 * modification to the data by these methods' returned {@link Action}s will be disregarded.</p>
 */
public interface ModifyingDataVisitor extends DataVisitor {
    /**
     * {@inheritDoc}
     *
     * <strong>Note: </strong> See the class javadocs for information regarding the use of
     * {@link #visit(int, DataVisitor, MappingDataContainer, SourceMetadata)} with visitors of this type.
     */
    default boolean visit(MappingDataContainer container, @Nullable SourceMetadata metadata) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    default boolean preVisit(DataType type) {
        return true;
    }

    /**
     * Visits and potentially modifies the data for a package.
     *
     * <p>A {@linkplain Action#delete() deletion action} will only affect the package information; any classes which are
     * within the package will not be affected.</p>
     *
     * @param packageData the package data
     * @return an action to take on the package data
     */
    default Action<PackageData> modifyPackage(PackageData packageData) {
        return Action.nothing();
    }

    /**
     * Visits and potentially modifies the data (and optional metadata) for a class.
     *
     * @param classData     the class data
     * @param classMetadata the class metadata, may be {@code null}
     * @return an action to take on the class data
     * @see DataType#CLASSES
     */
    default Action<ClassData> modifyClass(ClassData classData,
                                          @Nullable ClassMetadata classMetadata) {
        return Action.nothing();
    }

    /**
     * Visits and potentially modifies the data (and optional metadata) for a field.
     *
     * @param classData     the owning class's data
     * @param fieldData     the field data
     * @param classMetadata the owning class's metadata, may be {@code null}
     * @param fieldMetadata the field metadata, may be {@code null}
     * @return an action to take on the field data
     * @see DataType#FIELDS
     */
    default Action<FieldData> modifyField(ClassData classData, FieldData fieldData,
                                          @Nullable ClassMetadata classMetadata, @Nullable FieldMetadata fieldMetadata) {
        return Action.nothing();
    }

    /**
     * Visits and potentially modifies the data (and optional metadata) for a method.
     *
     * @param classData      the owning class's data
     * @param methodData     the method data
     * @param classMetadata  the owning class's metadata, may be {@code null}
     * @param methodMetadata the method metadata, may be {@code null}
     * @return an action to take on the method data
     * @see DataType#METHODS
     */
    default Action<MethodData> modifyMethod(ClassData classData, MethodData methodData,
                                            @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        return Action.nothing();
    }

    /**
     * Visits and potentially modifies the data for a method parameter.
     *
     * @param classData      the owning class's data
     * @param methodData     the owning method's data
     * @param paramData      the method parameter data
     * @param classMetadata  the owning class's metadata, may be {@code null}
     * @param methodMetadata the owning class's metadata, may be {@code null}
     * @return an action to take on the parameter data
     * @see DataType#PARAMETERS
     */
    default Action<ParameterData> modifyParameter(ClassData classData, MethodData methodData, ParameterData paramData,
                                                  @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        return Action.nothing();
    }

    /**
     * {@inheritDoc}
     */
    default void postVisit(DataType type) {
    }


    /**
     * Fully visits and potentially modifies a mapping data container and optional source metadata using a modifying
     * data visitor.
     *
     * @param revisitLimit the limit to the amount of times the data will be revisited; a limit of {@code 0} means the
     *                     data will not be revisited at all
     * @param visitor      the modifying data visitor
     * @param container    the mapping data container to be visited
     * @param metadata     the source metadata, may be {@code null}
     * @throws IllegalArgumentException if the revisit limit is negative
     */
    static void visit(int revisitLimit, ModifyingDataVisitor visitor, MappingDataBuilder container, @Nullable SourceMetadata metadata) {
        Preconditions.checkArgument(revisitLimit >= 0, "Revisit limit cannot be negative");
        DataVisitorHelper.visitModify(revisitLimit, visitor, container, metadata);
    }

    /**
     * Fully visit and potentially modifies s a mapping data container and optional source metadata using a modifying
     * data visitor. This has a revisit limit of {@link Integer#MAX_VALUE}.
     *
     * @param visitor   the modifying data visitor
     * @param container the mapping data container to be visited
     * @param metadata  the source metadata, may be {@code null}
     * @see #visit(int, ModifyingDataVisitor, MappingDataBuilder, SourceMetadata)
     */
    static void visit(ModifyingDataVisitor visitor, MappingDataBuilder container, @Nullable SourceMetadata metadata) {
        visit(Integer.MAX_VALUE, visitor, container, metadata);
    }

    // ********** Overridden methods from DataVisitor ********** //

    /**
     * {@inheritDoc}
     *
     * @deprecated Callers should use {@link #modifyPackage(MappingDataContainer.PackageData)} instead of this method
     * (which is also called by the default implementation of this method). The modification by that method as
     * represented in its {@link Action} is disregarded.
     */
    @Deprecated
    default void visitPackage(PackageData data) {
        modifyPackage(data);
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated Callers should use {@link #modifyClass(MappingDataContainer.ClassData, ClassMetadata)} instead of
     * this method (which is also called by the default implementation of this method). The modification by that method
     * as represented in its {@link Action} is disregarded.
     */
    @Deprecated
    default boolean visitClass(ClassData classData, @Nullable ClassMetadata classMetadata) {
        return !modifyClass(classData, classMetadata).skip;
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated Callers should use {@link #modifyField(MappingDataContainer.ClassData, MappingDataContainer.FieldData,
     * ClassMetadata, FieldMetadata)} instead of this method (which is also called by the default implementation of this
     * method). The modification by that method as represented in its {@link Action} is disregarded.
     */
    @Deprecated
    default void visitField(ClassData classData, FieldData fieldData,
                            @Nullable ClassMetadata classMetadata, @Nullable FieldMetadata fieldMetadata) {
        modifyField(classData, fieldData, classMetadata, fieldMetadata);
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated Callers should use {@link #modifyMethod(MappingDataContainer.ClassData, MappingDataContainer.MethodData,
     * ClassMetadata, MethodMetadata)} instead of this method (which is also called by the default implementation of
     * this method). The modification by that method as represented in its {@link Action} is disregarded.
     */
    @Deprecated
    default boolean visitMethod(ClassData classData, MethodData methodData,
                                @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        return !modifyMethod(classData, methodData, classMetadata, methodMetadata).skip;
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated Callers should use {@link #modifyParameter(MappingDataContainer.ClassData, MappingDataContainer.MethodData,
     * MappingDataContainer.ParameterData, ClassMetadata, MethodMetadata)} instead of this method (which is also called
     * by the default implementation of this method). The modification by that method as represented in its
     * {@link Action} is disregarded.
     */
    @Deprecated
    default void visitParameter(ClassData classData, MethodData methodData, ParameterData paramData,
                                @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        modifyParameter(classData, methodData, paramData, classMetadata, methodMetadata);
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
         * <p>The allowed modification to a target element is restricted to only modifications on the javadocs of the
         * target element. If more power is needed (such as to replace the element with a differently named one), use
         * {@link #replace(Object)} instead.</p>
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
         * Returns an action which replaces the target element with a new element and skips visiting its children
         * elements. The new element may have a different name from the target element, and may contain children elements
         * which are also copied over.
         *
         * <p>This action works like a combination of {@link #modify(Object, boolean)} and {@link #delete()}: the
         * original target element is deleted, and the new element is added with all of its children's data intact.
         * Because the original target element is deleted, its children elements are also deleted and therefore will
         * not be visited.</p>
         *
         * <p>Prefer using {@link #modify(Object, boolean)} if you only need to modify the data directly associated
         * with the element, as a replacement action has additional costs.</p>
         *
         * @param newData the new element which will replace the target element
         * @param <T> the type of the target and new element
         * @return an action which replaces the target element with a new element and skips visiting its children
         */
        public static <T> Action<T> replace(T newData) {
            Preconditions.checkNotNull(newData, "New data must not be null");
            return new Action<>(ActionType.REPLACE, true, newData);
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
            NOTHING(false),
            MODIFY(false),
            REPLACE(true),
            DELETE(true);

            final boolean removeExisting;

            ActionType(boolean removeExisting) {
                this.removeExisting = removeExisting;
            }
        }
    }
}
