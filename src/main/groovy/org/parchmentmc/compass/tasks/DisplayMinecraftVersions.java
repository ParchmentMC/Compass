package org.parchmentmc.compass.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.parchmentmc.feather.manifests.LauncherManifest;

public abstract class DisplayMinecraftVersions extends DefaultTask {
    @Input
    public abstract Property<LauncherManifest> getManifest();

    @TaskAction
    public void display() {
        Logger logger = getLogger();

        LauncherManifest manifest = getManifest().get();

        logger.lifecycle("Latest release: {}", manifest.getLatest().getRelease());
        logger.lifecycle("Latest snapshot: {}", manifest.getLatest().getSnapshot());

        logger.lifecycle("All versions ({}): {}", manifest.getVersions().size(),
                manifest.getVersions().stream().map(LauncherManifest.VersionData::getId).toArray());
    }
}
