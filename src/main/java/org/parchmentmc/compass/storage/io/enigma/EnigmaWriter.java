package org.parchmentmc.compass.storage.io.enigma;

import org.parchmentmc.feather.mapping.MappingDataContainer;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.parchmentmc.compass.storage.io.enigma.EnigmaFormattedExplodedIO.*;

// Helper package-only class, to separate writing
final class EnigmaWriter {
    private EnigmaWriter() { // Prevent instantiation
    }

    public static String stripToOuter(String className) {
        final int classSeparator = className.indexOf('$');
        if (classSeparator >= 0) {
            return className.substring(0, classSeparator);
        }
        return className;
    }

    public static String stripToMostInner(String className) {
        final int classSeparator = className.lastIndexOf('$');
        if (classSeparator >= 0) {
            return className.substring(classSeparator + 1);
        }
        return className;
    }

    public static Set<String> expandClass(String className) {
        if (className.indexOf('$') == -1) return Collections.emptySet();
        final Set<String> expandedClasses = new LinkedHashSet<>();
        String pkg = "";
        final int packageSeparator = className.lastIndexOf('/');
        if (packageSeparator > -1) {
            pkg = className.substring(0, packageSeparator + 1); // Include the /
            className = className.substring(packageSeparator + 1);
        }
        String prev = null;
        for (String classComponent : className.split("\\$")) {
            prev = (prev != null ? prev + "$" : "") + classComponent;
            expandedClasses.add(pkg + prev);
        }
        return expandedClasses;
    }

    public static Writer indent(Writer writer, int indent) throws IOException {
        for (int i = 0; i < indent; i++) {
            writer.append('\t');
        }
        return writer;
    }

    public static void writeClass(Writer writer, int indent, String name, MappingDataContainer.ClassData data) throws IOException {
        indent(writer, indent).append(CLASS).append(' ').append(name).append('\n');

        int memberIndent = indent + 1;
        int javadocIndent = indent + 2;

        for (String javadoc : data.getJavadoc()) {
            writeComment(writer, memberIndent, javadoc);
        }

        for (MappingDataContainer.FieldData field : data.getFields()) {
            indent(writer, memberIndent).append(FIELD).append(' ')
                    .append(field.getName()).append(' ').append(field.getDescriptor()).append('\n');

            for (String javadoc : field.getJavadoc()) {
                writeComment(writer, javadocIndent, javadoc);
            }
        }

        for (MappingDataContainer.MethodData method : data.getMethods()) {
            indent(writer, memberIndent).append(METHOD).append(' ')
                    .append(method.getName()).append(' ').append(method.getDescriptor()).append('\n');

            for (String javadoc : method.getJavadoc()) {
                writeComment(writer, javadocIndent, javadoc);
            }

            int paramIndent = memberIndent + 1;
            for (MappingDataContainer.ParameterData param : method.getParameters()) {
                if (param.getName() == null) continue; // Skip non-named parameters
                indent(writer, paramIndent).append(PARAM).append(' ')
                        .append(Byte.toString(param.getIndex())).append(' ').append(param.getName())
                        .append('\n');
                if (param.getJavadoc() != null) {
                    writeComment(writer, paramIndent + 1, param.getJavadoc());
                }
            }
        }

    }

    public static void writeComment(Writer writer, int indent, String comment) throws IOException {
        indent(writer, indent).append(COMMENT);
        if (!comment.isEmpty()) {
            writer.append(' ').append(comment);
        }
        writer.append('\n');
    }
}
