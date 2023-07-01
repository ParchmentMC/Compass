package org.parchmentmc.compass.providers;

import net.minecraftforge.srgutils.IMappingFile;
import org.gradle.api.provider.Provider;

/**
 * Provides the delegated mapping file as an intermediate format.
 */
public class DelegatingProvider extends IntermediateProvider {
    private final Provider<IMappingFile> provider;

    public DelegatingProvider(String name, Provider<IMappingFile> provider) {
        super(name);
        this.provider = provider;
    }

    @Override
    public IMappingFile getMapping() {
        return provider.get();
    }
}
