package org.parchmentmc.compass;

import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;

public class CompassExtension {
    private final Property<String> manifestURL;
    private final DirectoryProperty storageDirectory;
    private final Property<String> version;

    public CompassExtension(final Project project) {
        manifestURL = project.getObjects().property(String.class)
                .convention("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json");
        storageDirectory = project.getObjects().directoryProperty()
                .convention(project.getLayout().getProjectDirectory().dir("data"));
        version = project.getObjects().property(String.class);
    }

    public Property<String> getLauncherManifestURL() {
        return manifestURL;
    }

    public DirectoryProperty getStorageDirectory() {
        return storageDirectory;
    }

    public Property<String> getVersion() {
        return version;
    }
}
