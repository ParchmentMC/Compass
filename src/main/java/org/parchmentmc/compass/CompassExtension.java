package org.parchmentmc.compass;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.provider.Property;
import org.parchmentmc.compass.storage.io.MappingIOFormat;

public abstract class CompassExtension {
    public CompassExtension(final ProjectLayout layout) {
        getLauncherManifestURL().convention("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json");
        getProductionData().convention(layout.getProjectDirectory().dir("data"));
        getProductionDataFormat().convention(MappingIOFormat.MDC_EXPLODED);
        getStagingData().convention(layout.getProjectDirectory().dir("staging"));
        getStagingDataFormat().convention(MappingIOFormat.MDC_EXPLODED);
        getInputs().convention(layout.getProjectDirectory().dir("input"));
    }

    public abstract Property<String> getLauncherManifestURL();

    public abstract DirectoryProperty getProductionData();

    public abstract Property<MappingIOFormat> getProductionDataFormat();

    public abstract Property<String> getVersion();

    public abstract DirectoryProperty getStagingData();

    public abstract Property<MappingIOFormat> getStagingDataFormat();

    public abstract DirectoryProperty getInputs();
}
