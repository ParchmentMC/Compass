package org.parchmentmc.compass.storage.io;

import org.parchmentmc.feather.mapping.MappingDataContainer;
import org.parchmentmc.feather.mapping.VersionedMDCDelegate;
import org.parchmentmc.feather.mapping.VersionedMappingDataContainer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public interface MappingDataIO {
    void write(VersionedMappingDataContainer data, Path output) throws IOException;

    default void write(VersionedMappingDataContainer data, File output) throws IOException {
        write(data, output.toPath());
    }

    default void write(MappingDataContainer data, Path output) throws IOException {
        write(new VersionedMDCDelegate<>(VersionedMappingDataContainer.CURRENT_FORMAT, data), output);
    }

    default void write(MappingDataContainer data, File output) throws IOException {
        write(new VersionedMDCDelegate<>(VersionedMappingDataContainer.CURRENT_FORMAT, data), output);
    }

    VersionedMappingDataContainer read(Path input) throws IOException;

    default VersionedMappingDataContainer read(File input) throws IOException {
        return read(input.toPath());
    }
}
