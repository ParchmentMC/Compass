package org.parchmentmc.compass.storage.io.enigma;

import com.google.common.base.CharMatcher;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import org.parchmentmc.compass.storage.io.MappingDataIO;
import org.parchmentmc.compass.util.JSONUtil;
import org.parchmentmc.feather.mapping.*;
import org.parchmentmc.feather.util.SimpleVersion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.parchmentmc.compass.storage.io.enigma.EnigmaWriter.stripToMostInner;
import static org.parchmentmc.compass.storage.io.enigma.EnigmaWriter.writeClass;
import static org.parchmentmc.feather.mapping.MappingDataContainer.ClassData;

public class EnigmaFormattedExplodedIO implements MappingDataIO {
    public static final EnigmaFormattedExplodedIO LENGTH_SORT_INSTANCE = new EnigmaFormattedExplodedIO(JSONUtil.MOSHI,
            "    ", "mapping", true);
    public static final EnigmaFormattedExplodedIO LEXICOGRAPHIC_SORT_INSTANCE = new EnigmaFormattedExplodedIO(JSONUtil.MOSHI,
            "    ", "mapping", false);

    static final CharMatcher DOLLAR_SIGN = CharMatcher.is('$');
    static final String DOLLAR_SIGN_REGEX = "\\$";
    static final Comparator<String> CLASS_NAME_LENGTH_THEN_LEXICOGRAPHICALLY = Comparator
            .comparingInt(String::length)
            .thenComparing(Function.identity());

    static final String CLASS = "CLASS";
    static final String FIELD = "FIELD";
    static final String METHOD = "METHOD";
    static final String PARAM = "ARG";
    static final String COMMENT = "COMMENT";

    static final String VERSION_INFO_JSON = "info.json";
    static final String PACKAGES_DATA_JSON = "packages.json";

    private static final ParameterizedType PACKAGE_COLLECTION_TYPE =
            Types.newParameterizedType(Collection.class, Types.subtypeOf(MappingDataContainer.PackageData.class));

    private final Moshi moshi;
    private final String jsonIndent;
    private final String extension;
    // Whether to manually sort inner classes by inner class name length then lexicographically
    // If false, then sorting is lexicographically by the entire inner class FQN
    private final boolean lengthSort;

    public EnigmaFormattedExplodedIO(Moshi moshi, String jsonIndent, String extension, boolean lengthSort) {
        this.moshi = moshi;
        this.jsonIndent = jsonIndent;
        this.extension = extension;
        this.lengthSort = lengthSort;
    }

    @Override
    public void write(VersionedMappingDataContainer data, Path base) throws IOException {
        Files.createDirectories(base);

        // Write out version data
        try (BufferedSink sink = Okio.buffer(Okio.sink(base.resolve(VERSION_INFO_JSON)))) {
            DataInfo info = new DataInfo();
            info.version = data.getFormatVersion();

            moshi.adapter(DataInfo.class).indent(jsonIndent).toJson(sink, info);
        }

        // Write out packages.json
        try (BufferedSink sink = Okio.buffer(Okio.sink(base.resolve(PACKAGES_DATA_JSON)))) {
            moshi.adapter(PACKAGE_COLLECTION_TYPE).indent(jsonIndent).toJson(sink, data.getPackages());
        }

        // Group classes by their outermost classes (via `$` matching)
        final Supplier<Set<String>> setCreator = lengthSort
                ? () -> new TreeSet<>(EnigmaFormattedExplodedIO::compareClassNames)
                : TreeSet::new;
        final Map<String, Set<String>> outerClassesToClasses = data.getClasses().stream()
                .map(ClassData::getName)
                .sorted()
                .collect(Collectors.groupingBy(EnigmaWriter::stripToOuter,
                        Collectors.toCollection(setCreator)));

        Set<String> visited = new HashSet<>();

        Files.walkFileTree(base, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                final String fileName = file.getFileName().toString();
                if (file.getFileName().toString().endsWith('.' + extension)) {
                    final String outerClassName = extractClassNameFromPath(base.relativize(file));
                    final Set<String> classes = outerClassesToClasses.remove(outerClassName);

                    if (classes != null) {
                        // File corresponds to an outer class name, so overwrite contents and continue
                        writeClassFile(file, data, outerClassName, visited, classes);
                        return FileVisitResult.CONTINUE;
                    }
                } else if (base.relativize(file).getNameCount() == 1 && (VERSION_INFO_JSON.equals(fileName) || PACKAGES_DATA_JSON.equals(fileName))) {
                    // Do not delete the version info and packages data JSONs
                    return FileVisitResult.CONTINUE;
                }
                // Does not match a class, so delete
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                try (Stream<Path> stream = Files.list(dir)) {
                    if (!stream.findAny().isPresent()) {
                        // Empty directory, so delete
                        Files.delete(dir);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        // Write out classes without existing files
        for (Map.Entry<String, Set<String>> entry : outerClassesToClasses.entrySet()) {
            final String outerClass = entry.getKey();
            final Set<String> classes = entry.getValue();

            final Path mappingFile = base.resolve(outerClass + '.' + extension);

            writeClassFile(mappingFile, data, outerClass, visited, classes);
        }
    }

    // Parameter is a relative path from base directory
    private String extractClassNameFromPath(Path relativePath) {
        StringBuilder builder = new StringBuilder();

        for (Iterator<Path> iterator = relativePath.iterator(); iterator.hasNext(); ) {
            Path part = iterator.next();
            if (iterator.hasNext()) {
                builder.append(part.toString()).append('/');
            } else {
                // Filename, so remove extension (and dot) if present and don't append separator
                String filename = part.toString();
                if (filename.endsWith(this.extension)) {
                    // -1 to remove trailing dot (extension separator)
                    filename = filename.substring(0, filename.length() - this.extension.length() - 1);
                }
                builder.append(filename);
            }
        }

        return builder.toString();
    }

    private void writeClassFile(Path mappingFile, MappingDataContainer data, String outerClass, Set<String> visitedClasses, Set<String> classes) throws IOException {
        if (mappingFile.getParent() != null) {
            Files.createDirectories(mappingFile.getParent());
        }

        try (Writer writer = Files.newBufferedWriter(mappingFile)) {
            visitedClasses.add(outerClass);

            ClassData outerClassData = data.getClass(outerClass);
            // If the data for the outer class is not there, substitute an empty one
            if (outerClassData == null) outerClassData = new ImmutableMappingDataContainer.ImmutableClassData(
                    outerClass, Collections.emptyList(), Collections.emptyList(), Collections.emptyList()
            );

            writeClass(writer, 0, outerClass, outerClassData);

            for (String clz : classes) {
                if (clz.contentEquals(outerClass)) continue; // Skip the outer class
                visitedClasses.add(clz);

                for (String component : EnigmaWriter.expandClass(clz)) {
                    if (visitedClasses.contains(component)) continue; // Skip if it's already been visited
                    visitedClasses.add(component);
                    if (component.contentEquals(clz)) break; // Skip if it's the class currently being written

                    writeClass(writer, DOLLAR_SIGN.countIn(component), stripToMostInner(component),
                            new ImmutableMappingDataContainer.ImmutableClassData(
                                    component, Collections.emptyList(), Collections.emptyList(), Collections.emptyList()
                            ));
                }

                ClassData clzData = data.getClass(clz);
                // If the data for the inner class is not there, substitute an empty one
                if (clzData == null) clzData = new ImmutableMappingDataContainer.ImmutableClassData(
                        clz, Collections.emptyList(), Collections.emptyList(), Collections.emptyList()
                );

                writeClass(writer, DOLLAR_SIGN.countIn(clz), stripToMostInner(clz), clzData);
            }
        }
    }

    @Override
    public VersionedMappingDataContainer read(Path base) throws IOException {
        DataInfo info;
        try (BufferedSource source = Okio.buffer(Okio.source(base.resolve("info.json")))) {
            info = moshi.adapter(DataInfo.class).fromJson(source);
        }
        if (info == null) throw new IOException("info.json did not deserialize");

        Collection<? extends MappingDataContainer.PackageData> packages;
        try (BufferedSource source = Okio.buffer(Okio.source(base.resolve("packages.json")))) {
            packages = moshi.<Collection<? extends MappingDataContainer.PackageData>>adapter(PACKAGE_COLLECTION_TYPE).fromJson(source);
        }
        if (packages == null) throw new IOException("packages.json did not deserialize");

        MappingDataBuilder builder = new MappingDataBuilder();

        Files.walkFileTree(base, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                // Skip files not ending with the extension
                if (!file.toString().endsWith(extension)) return FileVisitResult.CONTINUE;

                try (BufferedReader reader = Files.newBufferedReader(file)) {
                    EnigmaReader.readFile(builder, reader);
                }

                return FileVisitResult.CONTINUE;
            }
        });

        return new VersionedMDCDelegate<>(info.version, new ImmutableMappingDataContainer(packages, builder.getClasses()));
    }

    static class DataInfo {
        public SimpleVersion version;
    }

    static int compareClassNames(String a, String b) {
        final String[] aComponents = a.split(DOLLAR_SIGN_REGEX);
        final String[] bComponents = b.split(DOLLAR_SIGN_REGEX);

        int ret = 0;
        int minimum = Math.min(aComponents.length, bComponents.length);
        for (int i = 0; i < minimum; i++) {
            String aComp = aComponents[i];
            String bComp = bComponents[i];
            ret = CLASS_NAME_LENGTH_THEN_LEXICOGRAPHICALLY.compare(aComp, bComp);
            if (ret != 0) break;
        }

        if (ret == 0) {
            ret = CLASS_NAME_LENGTH_THEN_LEXICOGRAPHICALLY.compare(a, b);
        }

        return ret;
    }
}
