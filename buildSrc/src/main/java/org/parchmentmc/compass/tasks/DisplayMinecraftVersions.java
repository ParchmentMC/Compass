package org.parchmentmc.compass.tasks;

import org.parchmentmc.compass.manifest.LauncherManifest;
import org.gradle.api.DefaultTask;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

public abstract class DisplayMinecraftVersions extends DefaultTask {
    @Input
    public abstract Property<LauncherManifest> getManifest();

    @TaskAction
    public void display() {
        Logger logger = getLogger();

        LauncherManifest manifest = getManifest().get();

        logger.lifecycle("Latest release: {}", manifest.latest.release);
        logger.lifecycle("Latest snapshot: {}", manifest.latest.snapshot);

        logger.lifecycle("All versions ({}): {}", manifest.versions.size(),
                manifest.versions.stream().map(d -> d.id).toArray());
    }
}
