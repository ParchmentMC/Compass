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
                        @Nullable FieldMetadata fieldMeta = MappingUtil.getFieldMetadata(classMeta, fieldData.getName());

                        visitor.visitField(classData, fieldData, classMeta, fieldMeta);
                    }

                    visitor.postVisit(DataType.FIELDS);
                }

                // Methods
                if (!visitor.preVisit(DataType.METHODS)) continue;

                for (MethodData methodData : classData.getMethods()) {
                    @Nullable MethodMetadata methodMeta = MappingUtil.getMethodMetadata(classMeta, methodData.getName(), methodData.getDescriptor());

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
        } while (visitCount < revisitLimit && visitor.revisit());
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
                    } else if (action.type == Action.ActionType.REPLACE && action.data != null) {
                        ctx.packagesToAdd.add(action.data);
                    }
                    if (action.type.removeExisting || packageData.getJavadoc().isEmpty()) {
                        ctx.packagesToRemove.add(packageData.getName());
                    }
                    // Ignore skip, as package data have no children
                }
                ctx.packagesToRemove.forEach(data::removePackage);
                ctx.packagesToRemove.clear();
                ctx.packagesToAdd.forEach(p -> data.getOrCreatePackage(p.getName()).addJavadoc(p.getJavadoc()));
                ctx.packagesToAdd.clear();

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
            ctx.classesToAdd.forEach(c -> copyClass(data.getOrCreateClass(c.getName()), c));
            ctx.classesToAdd.clear();

            visitor.postVisit(DataType.CLASSES);
        } while (visitCount < revisitLimit && visitor.revisit());
    }

    // Return true to delete
    private static boolean sanitizeClass(Context ctx, MutableClassData classData, @Nullable ClassMetadata classMeta) {
        final Action<ClassData> action = ctx.visitor.modifyClass(classData, classMeta);

        if (action.type == Action.ActionType.MODIFY && action.data != null) {
            classData.clearJavadoc().addJavadoc(action.data.getJavadoc());
        } else if (action.type == Action.ActionType.REPLACE && action.data != null) {
            ctx.classesToAdd.add(action.data);
        }

        if (!action.skip) {
            // Visit fields
            if (ctx.visitor.preVisit(DataType.FIELDS)) {
                for (MutableFieldData fieldData : classData.getFields()) {
                    if (sanitizeField(ctx, classData, fieldData, classMeta)) {
                        ctx.fieldsToRemove.add(fieldData.getName());
                    }
                }
                ctx.fieldsToRemove.forEach(classData::removeField);
                ctx.fieldsToRemove.clear();
                ctx.fieldsToAdd.forEach(f -> classData.createField(f.getName(), f.getDescriptor()).addJavadoc(f.getJavadoc()));
                ctx.fieldsToAdd.clear();

                ctx.visitor.postVisit(DataType.FIELDS);
            }

            // Visit methods
            if (ctx.visitor.preVisit(DataType.METHODS)) {
                for (MutableMethodData methodData : classData.getMethods()) {
                    if (sanitizeMethod(ctx, classData, methodData, classMeta)) {
                        ctx.methodsToRemove.add(new String[]{methodData.getName(), methodData.getDescriptor()});
                    }
                }
                ctx.methodsToRemove.forEach(arr -> classData.removeMethod(arr[0], arr[1]));
                ctx.methodsToRemove.clear();
                ctx.methodsToAdd.forEach(m -> copyMethod(classData.createMethod(m.getName(), m.getDescriptor()), m));
                ctx.methodsToAdd.clear();

                ctx.visitor.postVisit(DataType.METHODS);
            }
        }

        return action.type.removeExisting
                || (classData.getJavadoc().isEmpty() && classData.getFields().isEmpty() && classData.getMethods().isEmpty());
    }

    private static boolean sanitizeField(Context ctx, ClassData classData, MutableFieldData fieldData,
                                         @Nullable ClassMetadata classMeta) {
        final Action<FieldData> action = ctx.visitor.modifyField(classData, fieldData,
                classMeta, MappingUtil.getFieldMetadata(classMeta, fieldData.getName()));

        if (action.type == Action.ActionType.MODIFY && action.data != null) {
            fieldData.clearJavadoc().addJavadoc(action.data.getJavadoc());
        } else if (action.type == Action.ActionType.REPLACE && action.data != null) {
            ctx.fieldsToAdd.add(action.data);
        }

        return action.type.removeExisting
                || (fieldData.getJavadoc().isEmpty());
    }

    private static boolean sanitizeMethod(Context ctx, ClassData classData, MutableMethodData methodData,
                                          @Nullable ClassMetadata classMeta) {
        final MethodMetadata methodMeta = MappingUtil.getMethodMetadata(classMeta, methodData.getName(), methodData.getDescriptor());

        final Action<MethodData> action = ctx.visitor.modifyMethod(classData, methodData, classMeta, methodMeta);

        if (action.type == Action.ActionType.MODIFY && action.data != null) {
            methodData.clearJavadoc().addJavadoc(action.data.getJavadoc());
        } else if (action.type == Action.ActionType.REPLACE && action.data != null) {
            ctx.methodsToAdd.add(action.data);
        }

        if (!action.skip && ctx.visitor.preVisit(DataType.PARAMETERS)) {
            for (MutableParameterData paramData : methodData.getParameters()) {
                if (sanitizeParam(ctx, classData, methodData, paramData, classMeta, methodMeta)) {
                    ctx.paramsToRemove.add(paramData.getIndex());
                }
            }
            ctx.paramsToRemove.forEach(methodData::removeParameter);
            ctx.paramsToRemove.clear();
            ctx.paramsToAdd.forEach(p -> methodData.createParameter(p.getIndex()).setName(p.getName()).addJavadoc(p.getJavadoc()));
            ctx.paramsToAdd.clear();

            ctx.visitor.postVisit(DataType.PARAMETERS);
        }

        return action.type.removeExisting
                || (methodData.getJavadoc().isEmpty() && methodData.getParameters().isEmpty());
    }

    private static boolean sanitizeParam(Context ctx, ClassData classData, MethodData methodData, MutableParameterData paramData,
                                         @Nullable ClassMetadata classMeta, @Nullable MethodMetadata methodMeta) {

        final Action<ParameterData> action = ctx.visitor.modifyParameter(classData, methodData, paramData, classMeta, methodMeta);

        if (action.type == Action.ActionType.MODIFY && action.data != null) {
            paramData.setName(action.data.getName()).setJavadoc(action.data.getJavadoc());
        } else if (action.type == Action.ActionType.REPLACE && action.data != null) {
            ctx.paramsToAdd.add(action.data);
        }

        return action.type.removeExisting
                || (paramData.getName() == null && paramData.getJavadoc() == null);
    }

    private static void copyClass(MutableClassData target, ClassData origin) {
        target.clearJavadoc().addJavadoc(origin.getJavadoc());
        target.clearFields();
        origin.getFields().forEach(f -> target.createField(f.getName(), f.getDescriptor()).addJavadoc(f.getJavadoc()));
        target.clearMethods();
        origin.getMethods().forEach(m -> copyMethod(target.createMethod(m.getName(), m.getDescriptor()), m));
    }

    private static void copyMethod(MutableMethodData target, MethodData origin) {
        target.clearJavadoc().addJavadoc(origin.getJavadoc());
        target.clearParameters();
        origin.getParameters().forEach(p -> target.createParameter(p.getIndex()).setName(p.getName()).setJavadoc(p.getJavadoc()));
    }

    private static class Context {
        ModifyingDataVisitor visitor;
        Set<PackageData> packagesToAdd = new HashSet<>();
        Set<ClassData> classesToAdd = new HashSet<>();
        Set<FieldData> fieldsToAdd = new HashSet<>();
        Set<MethodData> methodsToAdd = new HashSet<>();
        Set<ParameterData> paramsToAdd = new HashSet<>();
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
