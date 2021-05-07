package org.parchmentmc.compass.storage.io;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import org.parchmentmc.compass.storage.ImmutableMappingDataContainer;
import org.parchmentmc.compass.storage.MappingDataContainer;
import org.parchmentmc.compass.util.JSONUtil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static com.squareup.moshi.Types.newParameterizedType;
import static com.squareup.moshi.Types.subtypeOf;

// Writes out the data as folders based on package
public class ExplodedDataIO implements MappingDataIO {
    public static final ExplodedDataIO INSTANCE = new ExplodedDataIO(JSONUtil.MOSHI, "  ");

    private final Moshi moshi;
    private final String indent;

    public ExplodedDataIO(Moshi moshi, String indent) {
        this.moshi = moshi;
        this.indent = indent;
    }

    private static final ParameterizedType PACKAGE_COLLECTION_TYPE =
            newParameterizedType(Collection.class, subtypeOf(MappingDataContainer.PackageData.class));
    private static final String EXTENSION = ".json";

    public void write(MappingDataContainer data, Path base) throws IOException {
        if (Files.exists(base)) {
            // noinspection ResultOfMethodCallIgnored
            Files.walk(base)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        Files.createDirectories(base);

        // Write out packages.json
        try (BufferedSink sink = Okio.buffer(Okio.sink(base.resolve("packages.json")))) {
            moshi.adapter(PACKAGE_COLLECTION_TYPE).indent(indent).toJson(sink, data.getPackages());
        }

        JsonAdapter<MappingDataContainer.ClassData> classAdapter = moshi.adapter(MappingDataContainer.ClassData.class).indent(indent);
        Path classesBase = base.resolve("classes");
        for (MappingDataContainer.ClassData classData : data.getClasses()) {
            String className = classData.getName() + EXTENSION;

            String json = classAdapter.toJson(classData);
            if (json.isEmpty()) continue;

            Path classPath = classesBase.resolve(className);
            if (classPath.getParent() != null && !Files.isDirectory(classPath.getParent())) {
                Files.createDirectories(classPath.getParent());
            }

            try (BufferedSink sink = Okio.buffer(Okio.sink(classPath))) {
                sink.writeUtf8(json);
            }
        }
    }

    public ImmutableMappingDataContainer read(Path base) throws IOException {
        Collection<? extends MappingDataContainer.PackageData> packages;
        try (BufferedSource source = Okio.buffer(Okio.source(base.resolve("packages.json")))) {
            packages = moshi.<Collection<? extends MappingDataContainer.PackageData>>adapter(PACKAGE_COLLECTION_TYPE).indent(indent).fromJson(source);
        }

        List<MappingDataContainer.ClassData> classes = new ArrayList<>();

        JsonAdapter<MappingDataContainer.ClassData> classAdapter = moshi.adapter(MappingDataContainer.ClassData.class).indent(indent);
        Path classesBase = base.resolve("classes");
        Files.walkFileTree(classesBase, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Objects.requireNonNull(file);
                Objects.requireNonNull(attrs);

                try (BufferedSource source = Okio.buffer(Okio.source(file))) {
                    classes.add(classAdapter.fromJson(source));
                }

                return FileVisitResult.CONTINUE;
            }
        });

        return new ImmutableMappingDataContainer(packages, classes);
    }
}
