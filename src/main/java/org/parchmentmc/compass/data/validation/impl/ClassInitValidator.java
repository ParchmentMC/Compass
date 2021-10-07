package org.parchmentmc.compass.data.validation.impl;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.data.validation.AbstractValidator;
import org.parchmentmc.compass.data.validation.ValidationIssue;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;

import java.util.function.Consumer;

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
    public void validate(Consumer<? super ValidationIssue> issueHandler, ClassData classData, MethodData methodData,
                         @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        if (methodData.getName().equals(CLASS_INITIALIZATION_METHOD_NAME)) {
            if (!methodData.getJavadoc().isEmpty()) {
                issueHandler.accept(error("Class/interface initialization method must not be documented"));
            }
        }
    }

    @Override
    public void validate(Consumer<? super ValidationIssue> issueHandler, ClassData classData, MethodData methodData,
                         ParameterData paramData, @Nullable ClassMetadata classMetadata,
                         @Nullable MethodMetadata methodMetadata) {
        if (methodData.getName().equals(CLASS_INITIALIZATION_METHOD_NAME)) {
            issueHandler.accept(error("There should be no parameters for the class/interface initialization method"));
        }
    }
}
