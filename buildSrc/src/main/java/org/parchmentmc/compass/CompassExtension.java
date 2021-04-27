package org.parchmentmc.compass;

import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;

import java.util.Arrays;
import java.util.Objects;

public class CompassExtension {
    private final Property<String> manifestURL;
    private final DirectoryProperty versionStorage;
    private final SetProperty<String> versions;
    private final Property<String> versionManifest;

    public CompassExtension(final Project project) {
        manifestURL = project.getObjects().property(String.class)
                .convention("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json");
        versionStorage = project.getObjects().directoryProperty()
                .convention(project.getLayout().getProjectDirectory().dir("versions"));
        versions = project.getObjects().setProperty(String.class)
                .convention(versionStorage.map(s -> Arrays.asList(Objects.requireNonNull(s.getAsFile().list()))));
        versionManifest = project.getObjects().property(String.class)
                .convention("manifest.json");
    }

    public Property<String> getManifestURL() {
        return manifestURL;
    }

    public DirectoryProperty getVersionStorage() {
        return versionStorage;
    }

    public SetProperty<String> getVersions() {
        return versions;
    }

    public Property<String> getVersionManifest() {
        return versionManifest;
    }
}
