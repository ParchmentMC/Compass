package org.parchmentmc.compass.data.visitation;

import org.checkerframework.checker.nullness.qual.Nullable;
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
 * A visitor for a {@link MappingDataContainer mapping data container}, with optional {@link SourceMetadata metadata}.
 *
 * @see #visit(DataVisitor, MappingDataContainer, SourceMetadata)
 */
public interface DataVisitor {
    /**
     * Called when starting to visit a mapping data container.
     *
     * <p>This should not be called manually by applications to visit a mapping data container.
     * Call {@link #visit(DataVisitor, MappingDataContainer, SourceMetadata)} instead.</p>
     *
     * @param container the mapping data container
     * @param metadata  the source metadata, may be {@code null}
     * @return {@code true} to continue visiting this container, {@code false} to skip
     */
    default boolean visit(MappingDataContainer container, @Nullable SourceMetadata metadata) {
        return true;
    }

    /**
     * Called before visiting the objects of a data type, to allow the visitor to skip visiting certain data types.
     *
     * <p>It should be noted that this is called everytime objects of a same type are visited, not five times only while
     * visiting. For example, {@link DataType#PARAMETERS} are fired for each method visited, not once only before the
     * first method.</p>
     *
     * @param type the data type about to be visited
     * @return {@code true} to continue visiting this data type, {@code false} to skip
     */
    default boolean preVisit(DataType type) {
        return true;
    }

    /**
     * Visits the data for a package.
     *
     * @param data the package data
     * @see DataType#PACKAGES
     */
    default void visitPackage(PackageData data) {
    }

    /**
     * Visits the data (and optional metadata) for a class.
     *
     * @param classData     the class data
     * @param classMetadata the class metadata, may be {@code null}
     * @return {@code true} to continue visiting the members of this class, {@code false} to skip
     * @see DataType#CLASSES
     */
    default boolean visitClass(ClassData classData, @Nullable ClassMetadata classMetadata) {
        return true;
    }

    /**
     * Visits the data (and optional metadata) for a field.
     *
     * @param classData     the owning class's data
     * @param fieldData     the field data
     * @param classMetadata the owning class's metadata, may be {@code null}
     * @param fieldMetadata the field metadata, may be {@code null}
     * @see DataType#FIELDS
     */
    default void visitField(ClassData classData, FieldData fieldData,
                            @Nullable ClassMetadata classMetadata, @Nullable FieldMetadata fieldMetadata) {
    }

    /**
     * Visits the data (and optional metadata) for a method.
     *
     * @param classData      the owning class's data
     * @param methodData     the method data
     * @param classMetadata  the owning class's metadata, may be {@code null}
     * @param methodMetadata the method metadata, may be {@code null}
     * @return {@code true} to continue visiting the parameters of this method, {@code false} to skip
     * @see DataType#METHODS
     */
    default boolean visitMethod(ClassData classData, MethodData methodData,
                                @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        return true;
    }

    /**
     * Visits the data for a method parameter.
     *
     * @param classData      the owning class's data
     * @param methodData     the owning method's data
     * @param paramData      the method parameter data
     * @param classMetadata  the owning class's metadata, may be {@code null}
     * @param methodMetadata the owning class's metadata, may be {@code null}
     * @see DataType#PARAMETERS
     */
    default void visitParameter(ClassData classData, MethodData methodData, ParameterData paramData,
                                @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
    }

    /**
     * Called after visiting the objects of a data type
     *
     * @param type the data type of the visited objects
     */
    default void postVisit(DataType type) {
    }

    /**
     * Fully visits a mapping data container and optional source metadata using a data visitor.
     *
     * @param visitor   the data visitor
     * @param container the mapping data container to be visited
     * @param metadata  the source metadata, may be {@code null}
     */
    static void visit(DataVisitor visitor, MappingDataContainer container, @Nullable SourceMetadata metadata) {
        DataVisitorHelper.visit(visitor, container, metadata);
    }

    /**
     * Type of the various objects visitable by a {@link DataVisitor}.
     */
    enum DataType {
        /**
         * Packages, represented by {@link MappingDataContainer.PackageData}.
         *
         * <p>Packages and classes are regarded as distinct in mapping data containers. Skipping the visitation of
         * packages does not mean that the classes within those packages are skipped from being visited.</p>
         *
         * @see #visitPackage(PackageData)
         */
        PACKAGES,
        /**
         * Classes, represented by {@link MappingDataContainer.ClassData} and {@link ClassMetadata}.
         *
         * <p>Skipping the visitation of a class means skipping the visitation of {@link #FIELDS fields} and
         * {@link #METHODS methods} of the class.</p>
         *
         * @see #visitClass(ClassData, ClassMetadata)
         */
        CLASSES,
        /**
         * Fields, represented by {@link MappingDataContainer.FieldData} and {@link FieldMetadata}.
         *
         * @see #visitField(ClassData, FieldData, ClassMetadata, FieldMetadata)
         */
        FIELDS,
        /**
         * Methods, represented by {@link MappingDataContainer.MethodData} and {@link MethodMetadata}.
         *
         * <p>Skipping the visitation of a method means skipping the visitation of the method's
         * {@link #PARAMETERS parameters}.</p>
         *
         * @see #visitMethod(ClassData, MethodData, ClassMetadata, MethodMetadata)
         */
        METHODS,
        /**
         * Method parameters, represented by {@link MappingDataContainer.ParameterData}.
         *
         * @see #visitParameter(ClassData, MethodData, ParameterData, ClassMetadata, MethodMetadata)
         */
        PARAMETERS
    }
}
