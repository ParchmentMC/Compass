package org.parchmentmc.compass.storage.io.enigma;

import org.parchmentmc.feather.mapping.MappingDataBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

import static org.parchmentmc.compass.storage.io.enigma.EnigmaFormattedExplodedIO.*;

// Helper package-only class, to separate reading
final class EnigmaReader {
    private EnigmaReader() { // Prevent instantiation
    }

    public static void readFile(MappingDataBuilder builder, BufferedReader reader) throws IOException {
        MappingDataBuilder.MutableClassData classData = null;
        MappingDataBuilder.MutableMethodData methodData = null;
        MappingDataBuilder.MutableHasJavadoc<?> javadoc = null;
        int prevClassIndent = -1;
        Deque<String> classNames = new ArrayDeque<>();

        String line;
        while ((line = reader.readLine()) != null) {
            final String[] tokens = line.trim().split("\\s");

            String firstToken = tokens[0].toUpperCase(Locale.ROOT);
            switch (firstToken) {
                case CLASS: {
                    int indent = countIndent(line);
                    String className = tokens[1];

                    for (int diff = prevClassIndent - indent; diff >= 0; diff--) {
                        classNames.pop();
                    }
                    prevClassIndent = indent;

                    if (!classNames.isEmpty()) { // Within a class
                        className = classNames.peek() + '$' + className;
                    }
                    classNames.push(className);
                    javadoc = classData = builder.createClass(className);

                    break;
                }
                case FIELD: {
                    if (classData == null) throw new IOException("Unexpected field line without class parent");
                    javadoc = classData.createField(tokens[1], tokens[2]);
                    break;
                }
                case METHOD: {
                    if (classData == null) throw new IOException("Unexpected method line without class parent");
                    javadoc = methodData = classData.createMethod(tokens[1], tokens[2]);
                    break;
                }
                case PARAM: {
                    if (methodData == null) throw new IOException("Unexpected arg line without method parent");
                    javadoc = methodData.createParameter(Byte.parseByte(tokens[1]))
                            .setName(tokens[2]);
                    break;
                }
                case COMMENT: {
                    if (javadoc == null)
                        throw new IOException("Unexpected comment line without javadoc-holding parent");
                    final ArrayList<String> strings = new ArrayList<>(Arrays.asList(tokens));
                    if (strings.size() > 0) {
                        strings.remove(0);
                    }
                    javadoc.addJavadoc(String.join(" ", strings));
                    break;
                }
            }
        }
    }

    public static int countIndent(String line) {
        int indent = 0;
        while (line.charAt(indent) == '\t') {
            indent++;
        }
        return indent;
    }
}
