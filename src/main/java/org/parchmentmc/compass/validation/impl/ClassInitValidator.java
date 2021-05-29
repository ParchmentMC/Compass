package org.parchmentmc.compass.validation.impl;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.validation.AbstractValidator;
import org.parchmentmc.compass.validation.ValidationIssue;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.parchmentmc.feather.mapping.MappingDataContainer.*;

/**
 * Validates that the class/interface initialization method ("{@code <clinit>}") is not documented, nor do any parameters exist.
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-2.html#jvms-2.9">The Java&reg; Virtual
 * Machine Specification, Java SE 8 Edition, &sect;2.9 "Special Methods"</a>
 */
public class ClassInitValidator extends AbstractValidator {
    private static final String CLASS_INITIALIZATION_METHOD_NAME = "<clinit>";

    public ClassInitValidator() {
        super("clinit");
    }

    @Override
    public List<? extends ValidationIssue> validate(ClassData classData, MethodData methodData,
                                                    @Nullable ClassMetadata classMetadata,
                                                    @Nullable MethodMetadata methodMetadata) {
        if (methodData.getName().equals(CLASS_INITIALIZATION_METHOD_NAME)) {
            if (!methodData.getJavadoc().isEmpty()) {
                return singletonList(error("Class/interface initialization method must not be documented"));
            }
        }

        return emptyList();
    }

    @Override
    public List<? extends ValidationIssue> validate(ClassData classData, MethodData methodData, ParameterData paramData,
                                                    @Nullable ClassMetadata classMetadata,
                                                    @Nullable MethodMetadata methodMetadata) {
        if (methodData.getName().equals(CLASS_INITIALIZATION_METHOD_NAME)) {
            return singletonList(error("There should be no parameters for the class/interface initialization method"));
        }

        return emptyList();
    }
}
