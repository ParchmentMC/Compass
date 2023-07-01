package org.parchmentmc.compass.storage.io;

import com.squareup.moshi.Moshi;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import org.parchmentmc.compass.util.JSONUtil;
import org.parchmentmc.feather.mapping.VersionedMappingDataContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class SingleFileDataIO implements MappingDataIO {
    public static final SingleFileDataIO INSTANCE = new SingleFileDataIO(JSONUtil.MOSHI, "  ");

    private final Moshi moshi;
    private final String indent;

    public SingleFileDataIO(Moshi moshi, String indent) {
        this.moshi = moshi;
        this.indent = indent;
    }

    @Override
    public void write(VersionedMappingDataContainer data, Path output) throws IOException {
        Files.deleteIfExists(output);
        if (output.getParent() != null) Files.createDirectories(output.getParent());

        try (BufferedSink sink = Okio.buffer(Okio.sink(output))) {
            moshi.adapter(VersionedMappingDataContainer.class).indent(indent).toJson(sink, data);
        }
    }

    @Override
    public VersionedMappingDataContainer read(Path input) throws IOException {
        try (BufferedSource source = Okio.buffer(Okio.source(input))) {
            VersionedMappingDataContainer data = moshi.adapter(VersionedMappingDataContainer.class)
                    .indent(indent).fromJson(source);
            Objects.requireNonNull(data, "Data from " + input + " was deserialized as null");
            return data;
        }
    }
}
