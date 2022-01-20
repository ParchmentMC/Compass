package org.parchmentmc.compass.data.visitation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.data.visitation.DataVisitor.DataType;
import org.parchmentmc.compass.data.visitation.ModifyingDataVisitor.Action;
import org.parchmentmc.compass.util.MappingUtil;
import org.parchmentmc.feather.mapping.MappingDataBuilder;
import org.parchmentmc.feather.mapping.MappingDataBuilder.MutablePackageData;
import org.parchmentmc.feather.mapping.MappingDataContainer;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.FieldMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;
import org.parchmentmc.feather.metadata.SourceMetadata;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.parchmentmc.feather.mapping.MappingDataBuilder.ClassData;
import static org.parchmentmc.feather.mapping.MappingDataBuilder.FieldData;
import static org.parchmentmc.feather.mapping.MappingDataBuilder.MethodData;
import static org.parchmentmc.feather.mapping.MappingDataBuilder.MutableClassData;
import static org.parchmentmc.feather.mapping.MappingDataBuilder.MutableFieldData;
import static org.parchmentmc.feather.mapping.MappingDataBuilder.MutableMethodData;
import static org.parchmentmc.feather.mapping.MappingDataBuilder.MutableParameterData;
import static org.parchmentmc.feather.mapping.MappingDataBuilder.PackageData;
import static org.parchmentmc.feather.mapping.MappingDataBuilder.ParameterData;
import static org.parchmentmc.feather.mapping.MappingDataBuilder.copyOf;

// Package-private helper class for the actual impl. for visiting the mapping data
class DataVisitorHelper {
    public static void visit(int revisitLimit, DataVisitor visitor, MappingDataContainer container,
                             @Nullable SourceMetadata metadata) {
        Map<String, ClassMetadata> classMetadataMap = null;
        int visitCount = 0;
        do {
            if (!visitor.visit(container, metadata)) return;
            visitCount++;

            // Packages
            if (visitor.preVisit(DataType.PACKAGES)) {
                for (PackageData packageData : container.getPackages()) {
                    visitor.visitPackage(packageData);
                }

                visitor.postVisit(DataType.PACKAGES);
            }

            if (!visitor.preVisit(DataType.CLASSES)) continue;

            if (classMetadataMap == null) { // Build the map once, only when required
                classMetadataMap = MappingUtil.buildClassMetadataMap(metadata);
            }

            // Classes
            for (ClassData classData : container.getClasses()) {
                @Nullable ClassMetadata classMeta = classMetadataMap.get(classData.getName());

                if (!visitor.visitClass(classData, classMeta)) continue;

                // Fields
                if (visitor.preVisit(DataType.FIELDS)) {
                    for (FieldData fieldData : classData.getFields()) {
                        @Nullable FieldMetadata fieldMeta = getFieldMetadata(classMeta, fieldData.getName());

                        visitor.visitField(classData, fieldData, classMeta, fieldMeta);
                    }

                    visitor.postVisit(DataType.FIELDS);
                }

                // Methods
                if (!visitor.preVisit(DataType.METHODS)) continue;

                for (MethodData methodData : classData.getMethods()) {
                    @Nullable MethodMetadata methodMeta = getMethodMetadata(classMeta, methodData.getName(), methodData.getDescriptor());

                    if (!visitor.visitMethod(classData, methodData, classMeta, methodMeta)) continue;

                    // Parameters
                    if (!visitor.preVisit(DataType.PARAMETERS)) continue;

                    for (ParameterData paramData : methodData.getParameters()) {
                        visitor.visitParameter(classData, methodData, paramData, classMeta, methodMeta);
                    }

                    visitor.postVisit(DataType.PARAMETERS);
                }

                visitor.postVisit(DataType.METHODS);
            }

            visitor.postVisit(DataType.CLASSES);
        } while (visitor.revisit() && visitCount < revisitLimit);
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

    // ModifyingDataVisitor

    public static void visitModify(int revisitLimit, ModifyingDataVisitor visitor, MappingDataBuilder data,
                                   @Nullable SourceMetadata metadata) {
        Map<String, ClassMetadata> classMetadataMap = null;
        Context ctx = new Context(visitor);
        int visitCount = 0;
        do {
            if (!visitor.visit(data, metadata)) return;
            visitCount++;
            ctx.reset(visitor);

            // Packages
            if (visitor.preVisit(DataType.PACKAGES)) {
                for (MutablePackageData packageData : data.getPackages()) {
                    final Action<PackageData> action = visitor.modifyPackage(packageData);

                    if (action.type == Action.ActionType.MODIFY && action.data != null) {
                        packageData.clearJavadoc().addJavadoc(action.data.getJavadoc());
                    }
                    if (action.type == Action.ActionType.DELETE || packageData.getJavadoc().isEmpty()) {
                        ctx.packagesToRemove.add(packageData.getName());
                    }
                    // Ignore skip, as package data have no children
                }
                ctx.packagesToRemove.forEach(data::removePackage);
                ctx.packagesToRemove.clear();

                visitor.postVisit(DataType.PACKAGES);
            }

            if (!visitor.preVisit(DataType.CLASSES)) continue;

            if (classMetadataMap == null) { // Build the map once, only when required
                classMetadataMap = MappingUtil.buildClassMetadataMap(metadata);
            }

            // Classes
            for (MutableClassData classData : data.getClasses()) {
                @Nullable ClassMetadata classMeta = classMetadataMap.get(classData.getName());

                if (sanitizeClass(ctx, classData, classMeta)) {
                    ctx.classesToRemove.add(classData.getName());
                }
            }
            ctx.classesToRemove.forEach(data::removeClass);
            ctx.classesToRemove.clear();

            visitor.postVisit(DataType.CLASSES);
        } while (visitor.revisit() && visitCount < revisitLimit);
    }

    // Return true to delete
    private static boolean sanitizeClass(Context ctx, MutableClassData classData, @Nullable ClassMetadata classMeta) {
        final Action<ClassData> action = ctx.visitor.modifyClass(classData, classMeta);

        if (action.type == Action.ActionType.MODIFY && action.data != null) {
            classData.clearJavadoc().addJavadoc(action.data.getJavadoc());
        }

        if (!action.skip) {
            // Visit fields
            if (!ctx.visitor.preVisit(DataType.FIELDS)) {
                for (MutableFieldData fieldData : classData.getFields()) {
                    if (sanitizeField(ctx, classData, fieldData, classMeta)) {
                        ctx.fieldsToRemove.add(fieldData.getName());
                    }
                }
                ctx.fieldsToRemove.forEach(classData::removeField);
                ctx.fieldsToRemove.clear();

                ctx.visitor.postVisit(DataType.FIELDS);
            }

            // Visit methods
            if (!ctx.visitor.preVisit(DataType.METHODS)) {
                for (MutableMethodData methodData : classData.getMethods()) {
                    if (sanitizeMethod(ctx, classData, methodData, classMeta)) {
                        ctx.methodsToRemove.add(new String[]{methodData.getName(), methodData.getDescriptor()});
                    }
                }
                ctx.methodsToRemove.forEach(arr -> classData.removeMethod(arr[0], arr[1]));
                ctx.methodsToRemove.clear();

                ctx.visitor.postVisit(DataType.METHODS);
            }
        }

        return action.type == Action.ActionType.DELETE
                || (classData.getJavadoc().isEmpty() && classData.getFields().isEmpty() && classData.getMethods().isEmpty());
    }

    private static boolean sanitizeField(Context ctx, ClassData classData, MutableFieldData fieldData,
                                         @Nullable ClassMetadata classMeta) {
        final Action<FieldData> action = ctx.visitor.modifyField(classData, fieldData,
                classMeta, getFieldMetadata(classMeta, fieldData.getName()));

        if (action.type == Action.ActionType.MODIFY && action.data != null) {
            fieldData.clearJavadoc().addJavadoc(action.data.getJavadoc());
        }

        return action.type == Action.ActionType.DELETE
                || (fieldData.getJavadoc().isEmpty());
    }

    private static boolean sanitizeMethod(Context ctx, ClassData classData, MutableMethodData methodData,
                                          @Nullable ClassMetadata classMeta) {
        final MethodMetadata methodMeta = getMethodMetadata(classMeta, methodData.getName(), methodData.getDescriptor());

        final Action<MethodData> action = ctx.visitor.modifyMethod(classData, methodData, classMeta, methodMeta);

        if (action.type == Action.ActionType.MODIFY && action.data != null) {
            methodData.clearJavadoc().addJavadoc(action.data.getJavadoc());
        }

        if (!action.skip && !ctx.visitor.preVisit(DataType.PARAMETERS)) {
            for (MutableParameterData paramData : methodData.getParameters()) {
                if (sanitizeParam(ctx, classData, methodData, paramData, classMeta, methodMeta)) {
                    ctx.paramsToRemove.add(paramData.getIndex());
                }
            }
            ctx.paramsToRemove.forEach(methodData::removeParameter);
            ctx.paramsToRemove.clear();

            ctx.visitor.postVisit(DataType.PARAMETERS);
        }

        return action.type == Action.ActionType.DELETE
                || (methodData.getJavadoc().isEmpty() && methodData.getParameters().isEmpty());
    }

    private static boolean sanitizeParam(Context ctx, ClassData classData, MethodData methodData, MutableParameterData paramData,
                                         @Nullable ClassMetadata classMeta, @Nullable MethodMetadata methodMeta) {

        final Action<ParameterData> action = ctx.visitor.modifyParameter(classData, methodData, paramData, classMeta, methodMeta);

        if (action.type == Action.ActionType.MODIFY && action.data != null) {
            paramData.setName(action.data.getName()).setJavadoc(action.data.getJavadoc());
        }

        return action.type == Action.ActionType.DELETE
                || (paramData.getName() == null && paramData.getJavadoc() == null);
    }

    private static class Context {
        ModifyingDataVisitor visitor;
        Set<String> packagesToRemove = new HashSet<>();
        Set<String> classesToRemove = new HashSet<>();
        Set<String> fieldsToRemove = new HashSet<>();
        // Each array has two elements: the method name, then the method descriptor
        Set<String[]> methodsToRemove = new HashSet<>();
        Set<Byte> paramsToRemove = new HashSet<>();

        public Context(ModifyingDataVisitor visitor) {
            reset(visitor);
        }

        public void reset(ModifyingDataVisitor visitor) {
            this.visitor = visitor;
            this.packagesToRemove.clear();
            this.classesToRemove.clear();
            this.fieldsToRemove.clear();
            this.methodsToRemove.clear();
            this.paramsToRemove.clear();
        }
    }
}
