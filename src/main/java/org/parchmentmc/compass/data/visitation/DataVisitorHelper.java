package org.parchmentmc.compass.data.visitation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.data.visitation.DataVisitor.DataType;
import org.parchmentmc.compass.util.MappingUtil;
import org.parchmentmc.feather.mapping.MappingDataContainer;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.FieldMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;
import org.parchmentmc.feather.metadata.SourceMetadata;

import java.util.Map;

// Package-private helper class for the actual impl. for visiting the mapping data
class DataVisitorHelper {
    public static void visit(DataVisitor visitor, MappingDataContainer container, @Nullable SourceMetadata metadata) {
        if (!visitor.visit(container, metadata)) return;

        // Packages
        if (visitor.preVisit(DataType.PACKAGES)) {
            for (MappingDataContainer.PackageData packageData : container.getPackages()) {
                visitor.visitPackage(packageData);
            }

            visitor.postVisit(DataType.PACKAGES);
        }

        if (!visitor.preVisit(DataType.CLASSES)) return;

        final Map<String, ClassMetadata> classMetadataMap = MappingUtil.buildClassMetadataMap(metadata);

        // Classes
        for (MappingDataContainer.ClassData classData : container.getClasses()) {
            @Nullable ClassMetadata classMeta = classMetadataMap.get(classData.getName());

            if (!visitor.visitClass(classData, classMeta)) continue;

            // Fields
            if (visitor.preVisit(DataType.FIELDS)) {
                for (MappingDataContainer.FieldData fieldData : classData.getFields()) {
                    @Nullable FieldMetadata fieldMeta = getFieldMetadata(classMeta, fieldData.getName());

                    visitor.visitField(classData, fieldData, classMeta, fieldMeta);
                }

                visitor.postVisit(DataType.FIELDS);
            }

            // Methods
            if (!visitor.preVisit(DataType.METHODS)) continue;

            for (MappingDataContainer.MethodData methodData : classData.getMethods()) {
                @Nullable MethodMetadata methodMeta = getMethodMetadata(classMeta, methodData.getName(), methodData.getDescriptor());

                if (!visitor.visitMethod(classData, methodData, classMeta, methodMeta)) continue;

                // Parameters
                if (!visitor.preVisit(DataType.PARAMETERS)) continue;

                for (MappingDataContainer.ParameterData paramData : methodData.getParameters()) {
                    visitor.visitParameter(classData, methodData, paramData, classMeta, methodMeta);
                }

                visitor.postVisit(DataType.PARAMETERS);
            }

            visitor.postVisit(DataType.METHODS);
        }

        visitor.postVisit(DataType.CLASSES);
    }

    @Nullable
    private static FieldMetadata getFieldMetadata(@Nullable ClassMetadata classMeta, String fieldName) {
        if (classMeta == null) return null;

        return classMeta.getFields().stream()
                .filter(s -> s.getName().getMojangName().orElse("").contentEquals(fieldName))
                .findFirst().orElse(null);
    }

    @Nullable
    private static MethodMetadata getMethodMetadata(@Nullable ClassMetadata classMeta, String methodName, String methodDesc) {
        if (classMeta == null) return null;

        return classMeta.getMethods().stream()
                .filter(s -> s.getName().getMojangName().orElse("").contentEquals(methodName)
                        && s.getDescriptor().getMojangName().orElse("").contentEquals(methodDesc))
                .findFirst().orElse(null);
    }
}
