package org.parchmentmc.compass.storage.io;

import org.parchmentmc.feather.mapping.MappingDataContainer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public interface MappingDataIO {
    void write(MappingDataContainer data, Path output) throws IOException;

    default void write(MappingDataContainer data, File output) throws IOException {
        write(data, output.toPath());
    }

    MappingDataContainer read(Path input) throws IOException;

    default MappingDataContainer read(File input) throws IOException {
        return read(input.toPath());
    }
}
