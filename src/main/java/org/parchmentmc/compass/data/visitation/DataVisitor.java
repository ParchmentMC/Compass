package org.parchmentmc.compass.data.visitation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.feather.mapping.MappingDataContainer;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.FieldMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;
import org.parchmentmc.feather.metadata.SourceMetadata;

import static org.parchmentmc.feather.mapping.MappingDataContainer.*;

public interface DataVisitor {
    default boolean visit(MappingDataContainer container, @Nullable SourceMetadata metadata) {
        return true;
    }

    default boolean preVisit(DataType type) {
        return true;
    }

    default void visitPackage(PackageData data) {
    }

    default boolean visitClass(ClassData classData, @Nullable ClassMetadata classMetadata) {
        return true;
    }

    default void visitField(ClassData classData, FieldData fieldData,
                            @Nullable ClassMetadata classMetadata, @Nullable FieldMetadata fieldMetadata) {
    }

    default boolean visitMethod(ClassData classData, MethodData methodData,
                                @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        return true;
    }

    default void visitParameter(ClassData classData, MethodData methodData, ParameterData paramData,
                                @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
    }

    default void postVisit(DataType type) {
    }

    static void visit(DataVisitor visitor, MappingDataContainer container, @Nullable SourceMetadata metadata) {
        DataVisitorHelper.visit(visitor, container, metadata);
    }

    enum DataType {
        PACKAGES,
        CLASSES,
        FIELDS,
        METHODS,
        PARAMETERS
    }
}
