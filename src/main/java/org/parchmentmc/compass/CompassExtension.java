package org.parchmentmc.compass;

import org.gradle.api.Action;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.parchmentmc.compass.storage.io.MappingIOFormat;

public abstract class CompassExtension {
    private final MigrationConfiguration migration;

    public CompassExtension(final ProjectLayout layout, final ObjectFactory objects) {
        getLauncherManifestURL().convention("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json");
        getProductionData().convention(layout.getProjectDirectory().dir("data"));
        getProductionDataFormat().convention(MappingIOFormat.ENIGMA_EXPLODED);
        getStagingData().convention(layout.getProjectDirectory().dir("staging"));
        getStagingDataFormat().convention(MappingIOFormat.ENIGMA_EXPLODED);
        getInputs().convention(layout.getProjectDirectory().dir("input"));

        this.migration = objects.newInstance(MigrationConfiguration.class, this);
    }

    public abstract Property<String> getLauncherManifestURL();

    public abstract DirectoryProperty getProductionData();

    public abstract Property<MappingIOFormat> getProductionDataFormat();

    public abstract Property<String> getVersion();

    public abstract DirectoryProperty getStagingData();

    public abstract Property<MappingIOFormat> getStagingDataFormat();

    public abstract DirectoryProperty getInputs();

    /**
     * The configuration for the data migration system.
     *
     * @return The configuration for the data migration system.
     * @see #migration(Action)
     */
    public MigrationConfiguration getMigration() {
        return this.migration;
    }

    /**
     * Configures the data migration system.
     *
     * @param configure The action or closure to configure the data migration system with.
     */
    public void migration(Action<? super MigrationConfiguration> configure) {
        configure.execute(this.migration);
    }
}
