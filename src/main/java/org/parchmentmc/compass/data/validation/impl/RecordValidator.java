package org.parchmentmc.compass.data.validation.impl;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.data.validation.Validator;
import org.parchmentmc.compass.util.MethodDescriptorVisitor;
import org.parchmentmc.feather.mapping.MappingDataContainer;
import org.parchmentmc.feather.mapping.MappingDataContainer.ClassData;
import org.parchmentmc.feather.mapping.MappingDataContainer.MethodData;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;
import org.parchmentmc.feather.metadata.RecordMetadata;
import org.parchmentmc.feather.metadata.WithName;
import org.parchmentmc.feather.metadata.WithType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Validates that the names of the parameters for the canonical constructor of a record class match the names of the
 * record's components.
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jls/se17/html/jls-8.html#jls-8.10.4.1">The Java&reg; Language
 * Specification, Java SE 17 Edition, &sect;8.10.4.1 "Normal Canonical Constructors"</a>
 */
public class RecordValidator extends Validator {
    private final Map<String, String> classesToCanonicalDescriptors = new HashMap<>();

    public RecordValidator() {
        super("record canonical constructor parameters");
    }

    @Override
    public boolean preVisit(DataType type) {
        return DataType.METHODS.test(type);
    }

    @Override
    public boolean visitMethod(ClassData classData, MethodData methodData,
                               @Nullable ClassMetadata classMeta, @Nullable MethodMetadata methodMeta) {
        if (classMeta == null || !classMeta.isRecord())
            return false; // We have metadata available and is a record class

        if (!methodData.getName().equals("<init>")) return false; // Is a constructor

        // Check if the method is the canonical constructor
        // The canonical constructor has the same amount and type of parameters as the record's components
        // Instead of manually checking each param type with the record component type (via field) and seeing if they match,
        // we shortcut by combining the record types into a param list for a constructor, and seeing if the method's
        // descriptor matches
        final String canonicalDesc = classesToCanonicalDescriptors.computeIfAbsent(classData.getName(),
            s -> createCanonicalConstructor(classMeta));

        if (!methodData.getDescriptor().equals(canonicalDesc))
            return false; // Matches expected descriptor for canonical ctor

        if (methodData.getParameters().isEmpty()) return false; // Fail-fast if there's no parameters mapped

        final List<String> recordNames = classMeta.getRecords().stream()
            .map(RecordMetadata::getField)
            .map(WithName::getName)
            .map(named -> named.getMojangName().orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        MethodDescriptorVisitor.visit(1, methodData.getDescriptor(), (position, index, type) -> {
            final MappingDataContainer.ParameterData param = methodData.getParameter(index);
            if (param != null && param.getName() != null) {
                final String expectedName = recordNames.get(position);
                if (!param.getName().equals(expectedName)) {
                    error("Parameter for canonical constructor named '" + param.getName()
                        + "' does not matched expected name of '" + expectedName + "'");
                }
            }
        });

        return false;
    }

    private static String createCanonicalConstructor(ClassMetadata classMeta) {
        final String params = classMeta.getRecords().stream()
            .map(RecordMetadata::getField)
            .map(WithType::getDescriptor)
            .map(named -> named.getMojangName().orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.joining(""));

        return "(" + params + ")V";
    }
}
