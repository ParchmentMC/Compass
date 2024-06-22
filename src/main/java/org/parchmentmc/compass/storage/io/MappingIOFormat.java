package org.parchmentmc.compass.storage.io;

import org.parchmentmc.compass.storage.io.enigma.EnigmaFormattedExplodedIO;
import org.parchmentmc.feather.mapping.VersionedMappingDataContainer;

import java.io.IOException;
import java.nio.file.Path;

public enum MappingIOFormat implements MappingDataIO {
    MDC_SINGLE(true, SingleFileDataIO.INSTANCE),
    MDC_EXPLODED(false, ExplodedDataIO.INSTANCE),
    ENIGMA_EXPLODED(false, EnigmaFormattedExplodedIO.LENGTH_SORT_INSTANCE),
    ENIGMA_EXPLODED_LENGTH_SORT(false, EnigmaFormattedExplodedIO.LENGTH_SORT_INSTANCE),
    ENIGMA_EXPLODED_LEXICOGRAPHIC_SORT(false, EnigmaFormattedExplodedIO.LEXICOGRAPHIC_SORT_INSTANCE);

    private final boolean fileBased;
    private final MappingDataIO dataIO;

    MappingIOFormat(boolean fileBased, MappingDataIO dataIO) {
        this.fileBased = fileBased;
        this.dataIO = dataIO;
    }

    public boolean isFileBased() {
        return fileBased;
    }

    public MappingDataIO getDataIO() {
        return dataIO;
    }

    @Override
    public void write(VersionedMappingDataContainer data, Path output) throws IOException {
        dataIO.write(data, output);
    }

    @Override
    public VersionedMappingDataContainer read(Path input) throws IOException {
        return dataIO.read(input);
    }
}
