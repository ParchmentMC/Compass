package org.parchmentmc.compass.sanitation;

import kotlin.Pair;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.sanitation.Sanitizer.Action;
import org.parchmentmc.compass.sanitation.Sanitizer.Action.ActionType;
import org.parchmentmc.compass.util.MappingUtil;
import org.parchmentmc.feather.mapping.MappingDataBuilder;
import org.parchmentmc.feather.mapping.MappingDataContainer;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.FieldMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;
import org.parchmentmc.feather.metadata.SourceMetadata;

import java.util.*;

import static org.parchmentmc.feather.mapping.MappingDataBuilder.*;

/**
 * A data sanitizer, which runs multiple {@link Sanitizer} on given input mapping data.
 *
 * <p>When sanitizing input data, all sanitizers are run serially on the data, in the order of their registration. A
 * sanitizer may request to run multiple passes against the data through the {@link Sanitizer#revisit()} method, however
 * there is a limit on how many revisits a sanitizer may request -- any further requests to revisit past that limit is
 * ignored.</p>
 */
public class DataSanitizer {
    private final Set<Sanitizer> sanitizers = new LinkedHashSet<>();
    private final int revisitLimit;

    public DataSanitizer() {
        this(5);
    }

    public DataSanitizer(int revisitLimit) {
        this.revisitLimit = revisitLimit;
    }

    public void addSanitizer(Sanitizer sanitizer) {
        sanitizers.add(sanitizer);
    }

    public boolean removeSanitizer(Sanitizer sanitizer) {
        return sanitizers.remove(sanitizer);
    }

    public Set<Sanitizer> getSanitizers() {
        return Collections.unmodifiableSet(sanitizers);
    }

    /**
     * Sanitizes the given input data and returns a copy of the sanitized data.
     *
     * <p>Most sanitizers are expected to make use of the source metadata, and therefore sanitation may not work
     * as expected if the metadata is not provided.</p>
     *
     * @param inputData the data to be sanitized
     * @param metadata  the metadata, may be {@code null}
     * @return the sanitized data
     */
    public MappingDataContainer validate(MappingDataContainer inputData, @Nullable SourceMetadata metadata) {
        final MappingDataBuilder workingData = copyOf(inputData);
        final Map<String, ClassMetadata> classMetadataMap = MappingUtil.buildClassMetadataMap(metadata);

        Context ctx = new Context();

        for (Sanitizer sanitizer : sanitizers) {
            ctx.sanitizer = sanitizer;

            int revisits = 0;

            do {
                if (!sanitizer.start(metadata != null)) break; // Skip if the sanitizer doesn't want to

                // **** Packages ****
                for (MutablePackageData packageData : workingData.getPackages()) {
                    final Action<PackageData> action = sanitizer.sanitize(packageData);
                    if (action.type == ActionType.DELETE) {
                        ctx.packagesToRemove.add(packageData.getName());
                    } else if (action.type == ActionType.MODIFY && action.data != null) {
                        packageData.clearJavadoc().addJavadoc(action.data.getJavadoc());
                        if (packageData.getJavadoc().isEmpty()) {
                            ctx.packagesToRemove.add(packageData.getName());
                        }
                    }
                    // Ignore skip, as package data have no children
                }
                ctx.packagesToRemove.forEach(workingData::removePackage);
                ctx.packagesToRemove.clear();

                // **** Classes ****
                for (MutableClassData classData : workingData.getClasses()) {
                    final ClassMetadata classMeta = classMetadataMap.get(classData.getName());
                    if (sanitizeClass(ctx, classData, classMeta)) {
                        ctx.classesToRemove.add(classData.getName());
                    }
                }
                ctx.classesToRemove.forEach(workingData::removeClass);
                ctx.classesToRemove.clear();

            } while (revisits++ < revisitLimit && sanitizer.revisit());

        }

        throw new UnsupportedOperationException("Not yet implemented");
    }

    // Return true to delete
    private boolean sanitizeClass(Context ctx,
                                  MutableClassData classData, @Nullable ClassMetadata classMeta) {
        final Action<ClassData> action = ctx.sanitizer.sanitize(classData, classMeta);

        if (action.type == ActionType.MODIFY && action.data != null) {
            classData.clearJavadoc().addJavadoc(action.data.getJavadoc());
        }

        if (!action.skip) {
            // Visit fields
            for (MutableFieldData fieldData : classData.getFields()) {
                if (sanitizeField(ctx, classData, fieldData, classMeta)) {
                    ctx.fieldsToRemove.add(fieldData.getName());
                }
            }
            ctx.fieldsToRemove.forEach(classData::removeField);
            ctx.fieldsToRemove.clear();

            // Visit methods
            for (MutableMethodData methodData : classData.getMethods()) {
                if (sanitizeMethod(ctx, classData, methodData, classMeta)) {
                    ctx.methodsToRemove.add(new Pair<>(methodData.getName(), methodData.getDescriptor()));
                }
            }
            ctx.methodsToRemove.forEach(pair -> classData.removeMethod(pair.getFirst(), pair.getSecond()));
            ctx.methodsToRemove.clear();
        }

        return action.type == ActionType.DELETE || (classData.getJavadoc().isEmpty()
                && classData.getFields().isEmpty() && classData.getMethods().isEmpty());
    }

    private boolean sanitizeField(Context ctx, ClassData classData, MutableFieldData fieldData,
                                  @Nullable ClassMetadata classMeta) {

        final FieldMetadata fieldMeta = classMeta != null ? classMeta.getFields().stream()
                .filter(s -> s.getName().getMojangName().orElse("").contentEquals(fieldData.getName()))
                .findFirst().orElse(null) : null;

        final Action<FieldData> action = ctx.sanitizer.sanitize(classData, fieldData, classMeta, fieldMeta);

        if (action.type == ActionType.MODIFY && action.data != null) {
            fieldData.clearJavadoc().addJavadoc(action.data.getJavadoc());
        }

        return action.type == ActionType.DELETE || (fieldData.getJavadoc().isEmpty());
    }

    private boolean sanitizeMethod(Context ctx, ClassData classData, MutableMethodData methodData,
                                   @Nullable ClassMetadata classMeta) {
        final MethodMetadata methodMeta = classMeta != null ? classMeta.getMethods().stream()
                .filter(s -> s.getName().getMojangName().orElse("").contentEquals(methodData.getName())
                        && s.getDescriptor().getMojangName().orElse("").contentEquals(methodData.getDescriptor()))
                .findFirst().orElse(null) : null;

        final Action<MethodData> action = ctx.sanitizer.sanitize(classData, methodData, classMeta, methodMeta);

        if (action.type == ActionType.MODIFY && action.data != null) {
            methodData.clearJavadoc().addJavadoc(action.data.getJavadoc());
        }

        if (!action.skip) {
            for (MutableParameterData paramData : methodData.getParameters()) {
                if (sanitizeParam(ctx, classData, methodData, paramData, classMeta, methodMeta)) {
                    ctx.paramsToRemove.add(paramData.getIndex());
                }
            }
            ctx.paramsToRemove.forEach(methodData::removeParameter);
            ctx.paramsToRemove.clear();
        }

        return action.type == ActionType.DELETE || (methodData.getJavadoc().isEmpty() && methodData.getParameters().isEmpty());
    }

    private boolean sanitizeParam(Context ctx, ClassData classData, MethodData methodData, MutableParameterData paramData,
                                  @Nullable ClassMetadata classMeta, @Nullable MethodMetadata methodMeta) {

        final Action<ParameterData> action = ctx.sanitizer.sanitize(classData, methodData, paramData, classMeta, methodMeta);

        if (action.type == ActionType.MODIFY && action.data != null) {
            paramData.setName(action.data.getName()).setJavadoc(action.data.getJavadoc());
        }

        return action.type == ActionType.DELETE || (paramData.getName() == null && paramData.getJavadoc() == null);
    }

    private static class Context {
        Sanitizer sanitizer;
        Set<String> packagesToRemove = new HashSet<>();
        Set<String> classesToRemove = new HashSet<>();
        Set<String> fieldsToRemove = new HashSet<>();
        Set<Pair<String, String>> methodsToRemove = new HashSet<>();
        Set<Byte> paramsToRemove = new HashSet<>();
    }
}
