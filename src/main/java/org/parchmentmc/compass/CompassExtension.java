package org.parchmentmc.compass;

import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.parchmentmc.compass.storage.io.MappingIOFormat;

public class CompassExtension {
    private final Property<String> manifestURL;
    private final DirectoryProperty productionData;
    private final Property<MappingIOFormat> productionDataFormat;
    private final Property<String> version;
    private final DirectoryProperty stagingData;
    private final Property<MappingIOFormat> stagingDataFormat;
    private final DirectoryProperty inputs;

    public CompassExtension(final Project project) {
        manifestURL = project.getObjects().property(String.class)
                .convention("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json");
        productionData = project.getObjects().directoryProperty()
                .convention(project.getLayout().getProjectDirectory().dir("data"));
        productionDataFormat = project.getObjects().property(MappingIOFormat.class)
                .convention(MappingIOFormat.MDC_EXPLODED);
        version = project.getObjects().property(String.class);
        stagingData = project.getObjects().directoryProperty()
                .convention(project.getLayout().getProjectDirectory().dir("staging"));
        stagingDataFormat = project.getObjects().property(MappingIOFormat.class)
                .convention(MappingIOFormat.MDC_EXPLODED);
        inputs = project.getObjects().directoryProperty()
                .convention(project.getLayout().getProjectDirectory().dir("input"));
    }

    public Property<String> getLauncherManifestURL() {
        return manifestURL;
    }

    public DirectoryProperty getProductionData() {
        return productionData;
    }

    public Property<MappingIOFormat> getProductionDataFormat() {
        return productionDataFormat;
    }

    public Property<String> getVersion() {
        return version;
    }

    public DirectoryProperty getStagingData() {
        return stagingData;
    }

    public Property<MappingIOFormat> getStagingDataFormat() {
        return stagingDataFormat;
    }

    public DirectoryProperty getInputs() {
        return inputs;
    }
}
