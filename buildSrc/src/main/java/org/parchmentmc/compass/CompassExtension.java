package org.parchmentmc.compass;

import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;

public class CompassExtension {
    private final Property<String> manifestURL;
    private final DirectoryProperty versionStorage;

    public CompassExtension(final Project project) {
        manifestURL = project.getObjects().property(String.class)
                .convention("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json");
        versionStorage = project.getObjects().directoryProperty()
                .convention(project.getLayout().getProjectDirectory().dir("versions"));
    }

    public Property<String> getManifestURL() {
        return manifestURL;
    }

    public DirectoryProperty getVersionStorage() {
        return versionStorage;
    }
}
