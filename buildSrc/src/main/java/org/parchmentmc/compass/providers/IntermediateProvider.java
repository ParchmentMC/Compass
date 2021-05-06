package org.parchmentmc.compass.providers;

import net.minecraftforge.srgutils.IMappingFile;
import org.gradle.api.Named;
import org.parchmentmc.compass.CompassPlugin;

import java.io.IOException;

/**
 * Provides a mapping file which contains an intermediate format.
 *
 * @see CompassPlugin#getIntermediates()
 */
public abstract class IntermediateProvider implements Named {
    protected final String name;

    protected IntermediateProvider(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Returns a mapping file, formatted for {@literal obfuscated names -> intermediate names}.
     *
     * @return A mapping file for obfuscated to intermediate names
     * @throws IOException if there is an exception while loading or creating the mapping file
     */
    public abstract IMappingFile getMapping() throws IOException;
}
