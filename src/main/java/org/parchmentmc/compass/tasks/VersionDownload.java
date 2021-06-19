package org.parchmentmc.compass.tasks;

import de.undercouch.gradle.tasks.download.DownloadAction;
import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.parchmentmc.compass.CompassExtension;
import org.parchmentmc.compass.CompassPlugin;
import org.parchmentmc.feather.manifests.VersionManifest;

import java.io.File;
import java.io.IOException;

import static org.parchmentmc.compass.util.download.DownloadUtil.createAndExecuteAction;
import static org.parchmentmc.compass.util.download.DownloadUtil.verifyChecksum;

public abstract class VersionDownload extends DefaultTask {
    private final Provider<RegularFile> output;

    public VersionDownload() {
        CompassPlugin plugin = getProject().getPlugins().getPlugin(CompassPlugin.class);
        CompassExtension extension = getProject().getExtensions().getByType(CompassExtension.class);

        getManifest().convention(plugin.getManifestsDownloader().getVersionManifest());
        getDownloadKey().convention("client");
        getDestinationDirectory().convention(getProject().getLayout().getBuildDirectory().dir("downloads"));
        getFileName().convention(extension.getVersion().zip(getDownloadKey(), (ver, key) -> ver + '-' + key + ".jar"));
        output = getDestinationDirectory().file(getFileName());
    }

    @Input
    public abstract Property<VersionManifest> getManifest();

    @Input
    public abstract Property<String> getDownloadKey();

    @Internal("Represented as part of outputFile")
    public abstract DirectoryProperty getDestinationDirectory();

    @Internal("Represented as part of outputFile")
    public abstract Property<String> getFileName();

    @OutputFile
    public Provider<RegularFile> getOutputFile() {
        return output;
    }

    @TaskAction
    public void download() throws IOException {
        VersionManifest manifest = getManifest().get();
        String key = getDownloadKey().get();
        File output = getOutputFile().get().getAsFile();

        VersionManifest.DownloadInfo info = manifest.getDownloads().get(key);
        if (info == null) {
            throw new InvalidUserDataException("No download info for key " + key);
        }

        DownloadAction action = createAndExecuteAction(getProject(), info.getUrl(), output, "download entry for key " + key);
        verifyChecksum(output, info.getSHA1(), "download entry for key " + key);

        setDidWork(!action.isUpToDate());
    }
}
