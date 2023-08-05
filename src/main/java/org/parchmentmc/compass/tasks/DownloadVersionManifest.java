package org.parchmentmc.compass.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.parchmentmc.compass.CompassPlugin;
import org.parchmentmc.feather.manifests.LauncherManifest;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import static org.parchmentmc.compass.util.download.DownloadUtil.areNotChecksumsEqual;
import static org.parchmentmc.compass.util.download.DownloadUtil.createAndExecuteAction;
import static org.parchmentmc.compass.util.download.DownloadUtil.verifyChecksum;

public abstract class DownloadVersionManifest extends DefaultTask {
    @Input
    public abstract Property<LauncherManifest> getLauncherManifest();

    @Input
    public abstract Property<String> getVersion();

    @OutputFile
    public abstract RegularFileProperty getVersionManifest();

    @Inject
    public DownloadVersionManifest(final ProjectLayout layout) {
        final CompassPlugin plugin = getProject().getPlugins().getPlugin(CompassPlugin.class);

        getLauncherManifest().convention(plugin.getManifestsDownloader().getLauncherManifest());

        getVersionManifest().convention(layout.getBuildDirectory().dir("manifests")
                .zip(getVersion(), (d, v) -> d.file(v + ".json")));
    }

    @TaskAction
    public void execute() {
        final String version = getVersion().get();
        final LauncherManifest.VersionData versionData = getLauncherManifest().get().getVersions().stream()
                .filter(str -> str.getId().equals(version))
                .findFirst()
                .orElseThrow(() -> new InvalidUserDataException("No version data found for " + version));

        final File outputFile = getVersionManifest().get().getAsFile();

        // Only download if our expected hash (from the launcher manifest) is different from our on-disk file
        if (!outputFile.exists() || areNotChecksumsEqual(outputFile, versionData.getSHA1())) {
            try {
                createAndExecuteAction(this.getProject(), versionData.getUrl(), outputFile, "version manifest");
                verifyChecksum(outputFile, versionData.getSHA1(), "version manifest");
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to download version manifest for " + version, e);
            }
            setDidWork(true);
        }
    }
}
