package org.parchmentmc.compass.storage.io;

import com.squareup.moshi.Moshi;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import org.parchmentmc.compass.storage.MappingDataContainer;
import org.parchmentmc.compass.util.JSONUtil;

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
    public void write(MappingDataContainer data, Path output) throws IOException {
        Files.deleteIfExists(output);
        if (output.getParent() != null) Files.createDirectories(output.getParent());

        try (BufferedSink sink = Okio.buffer(Okio.sink(output))) {
            moshi.adapter(MappingDataContainer.class).indent(indent).toJson(sink, data);
        }
    }

    @Override
    public MappingDataContainer read(Path input) throws IOException {
        try (BufferedSource source = Okio.buffer(Okio.source(input))) {
            MappingDataContainer data = moshi.adapter(MappingDataContainer.class).indent(indent).fromJson(source);
            Objects.requireNonNull(data, "Data from " + input + " was deserialized as null");
            return data;
        }
    }
}
