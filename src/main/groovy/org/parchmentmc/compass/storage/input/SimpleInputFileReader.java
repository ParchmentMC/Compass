package org.parchmentmc.compass.storage.input;

import org.parchmentmc.feather.mapping.MappingDataBuilder;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reader for a simple and quickly editable input file format.
 *
 * @see <a href="https://github.com/ParchmentMC/Compass/wiki/Simple-Input-File-Format"><tt>ParchmentMC/Compass</tt>
 * repository wiki, "Simple Input File Format"</a>
 */
public class SimpleInputFileReader {
    static final Pattern DEFINITION_WORD = Pattern.compile("^(package|class|field|method|param) ");

    public static void parseLines(MappingDataBuilder builder, List<String> lines) throws IOException {
        Matcher defWordMatcher = DEFINITION_WORD.matcher("");

        MappingDataBuilder.MutableHasJavadoc<?> javadoc = null;
        MappingDataBuilder.MutableClassData classData = null;
        MappingDataBuilder.MutableMethodData methodData = null;
        for (int i = 0, linesSize = lines.size(); i < linesSize; i++) {
            String line = lines.get(i).trim();

            int commentIndex = line.indexOf('#');
            if (commentIndex != -1) {
                line = line.substring(0, commentIndex).trim();
                if (line.isEmpty()) continue; // Skip lines with only comments
            }

            if (defWordMatcher.reset(line).find()) {
                String word = defWordMatcher.group(1);
                String[] split = line.split(" ");
                switch (word) {
                    case "package": {
                        if (split.length != 2)
                            throw new IOException("Invalid package line at #" + i + "; incorrect no. of tokens: " + line);
                        String name = split[1];
                        javadoc = builder.createPackage(name);
                        break;
                    }
                    case "class": {
                        if (split.length != 2)
                            throw new IOException("Invalid class line at #" + i + "; incorrect no. of tokens: " + line);
                        String name = split[1];
                        classData = builder.createClass(name);
                        javadoc = classData;
                        break;
                    }
                    case "field": {
                        if (split.length != 3)
                            throw new IOException("Invalid field line at #" + i + "; incorrect no. of tokens: " + line);
                        if (classData == null)
                            throw new IOException("Invalid field line at #" + i + "; No enclosing class: " + line);
                        String name = split[1];
                        String descriptor = split[2];
                        javadoc = classData.createField(name, descriptor);
                        break;
                    }
                    case "method": {
                        if (split.length != 3)
                            throw new IOException("Invalid method line at #" + i + "; incorrect no. of tokens: " + line);
                        if (classData == null)
                            throw new IOException("Invalid method line at #" + i + "; No enclosing class: " + line);
                        String name = split[1];
                        String descriptor = split[2];
                        methodData = classData.createMethod(name, descriptor);
                        javadoc = methodData;
                        break;
                    }
                    case "param": {
                        if (split.length < 2 || split.length > 3)
                            throw new IOException("Invalid param line at #" + i + "; incorrect no. of tokens: " + line);
                        if (methodData == null)
                            throw new IOException("Invalid param line at #" + i + "; No enclosing method: " + line);
                        byte index = Byte.parseByte(split[1]);
                        String name = split.length == 3 ? split[2] : null;
                        javadoc = methodData.createParameter(index).setName(name);
                        break;
                    }
                    default: {
                        throw new IOException("Unrecognized token " + word + " at #" + i + ": " + line);
                    }
                }
            } else {
                if (javadoc == null)
                    throw new IOException("Invalid documentation line at #" + i + "; No enclosing definition: " + line);
                javadoc.addJavadoc(line);
            }
        }
    }
}
