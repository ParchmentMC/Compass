package org.parchmentmc.compass;

import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;

public class CompassExtension {
    private final Property<String> manifestURL;
    private final DirectoryProperty productionData;
    private final Property<String> version;
    private final DirectoryProperty stagingData;
    private final DirectoryProperty inputs;

    public CompassExtension(final Project project) {
        manifestURL = project.getObjects().property(String.class)
                .convention("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json");
        productionData = project.getObjects().directoryProperty()
                .convention(project.getLayout().getProjectDirectory().dir("data"));
        version = project.getObjects().property(String.class);
        stagingData = project.getObjects().directoryProperty()
                .convention(project.getLayout().getProjectDirectory().dir("staging"));
        inputs = project.getObjects().directoryProperty()
                .convention(project.getLayout().getProjectDirectory().dir("input"));
    }

    public Property<String> getLauncherManifestURL() {
        return manifestURL;
    }

    public DirectoryProperty getProductionData() {
        return productionData;
    }

    public Property<String> getVersion() {
        return version;
    }

    public DirectoryProperty getStagingData() {
        return stagingData;
    }

    public DirectoryProperty getInputs() {
        return inputs;
    }
}
