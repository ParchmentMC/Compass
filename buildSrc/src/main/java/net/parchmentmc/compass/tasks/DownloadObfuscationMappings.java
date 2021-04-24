package net.parchmentmc.compass.tasks;

import de.undercouch.gradle.tasks.download.DownloadAction;
import net.parchmentmc.compass.manifest.VersionManifest;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;

public abstract class DownloadObfuscationMappings extends DefaultTask {
    @Input
    public abstract Property<VersionManifest> getVersionManifest();

    @Input
    public abstract Property<String> getClientMappingsKey();

    @Input
    public abstract Property<String> getServerMappingsKey();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @OutputFile
    public abstract RegularFileProperty getClientMappings();

    @OutputFile
    public abstract RegularFileProperty getServerMappings();

    public DownloadObfuscationMappings() {
        // See VersionManifest#downloads
        getClientMappingsKey().convention("client_mappings");
        getServerMappingsKey().convention("server_mappings");

        getOutputDirectory().convention(getProject().getLayout().getBuildDirectory().dir(getName()));
        getClientMappings().convention(getOutputDirectory().file("client.txt"));
        getServerMappings().convention(getOutputDirectory().file("server.txt"));
    }

    @TaskAction
    public void download() {
        VersionManifest manifest = getVersionManifest().get();
        String clientMappingsKey = getClientMappingsKey().get();
        String serverMappingsKey = getServerMappingsKey().get();

        VersionManifest.DownloadInfo clientMappingsDL = manifest.downloads.get(clientMappingsKey);
        VersionManifest.DownloadInfo serverMappingsDL = manifest.downloads.get(serverMappingsKey);
        if (clientMappingsDL == null) {
            throw new IllegalStateException("Missing client obfuscation mappings for " + manifest.id + " (key: " + clientMappingsKey + ")");
        }
        if (serverMappingsDL == null) {
            throw new IllegalStateException("Missing server obfuscation mappings for " + manifest.id + " (key: " + serverMappingsKey + ")");
        }

        File clientOutput = ensureAvailable(getClientMappings().get().getAsFile());
        File serverOutput = ensureAvailable(getServerMappings().get().getAsFile());

        DownloadAction clientDL = new DownloadAction(getProject());
        clientDL.quiet(true);
        clientDL.overwrite(true); // Always check
        clientDL.onlyIfModified(true); // Only re-download if changed
        clientDL.useETag(true); // Use ETag to additionally check for changes
        clientDL.src(clientMappingsDL.url);
        clientDL.dest(clientOutput);

        DownloadAction serverDL = new DownloadAction(getProject());
        serverDL.quiet(true);
        serverDL.overwrite(true); // Always check
        serverDL.onlyIfModified(true); // Only re-download if changed
        serverDL.useETag(true); // Use ETag to additionally check for changes
        serverDL.src(serverMappingsDL.url);
        serverDL.dest(serverOutput);

        try {
            clientDL.execute();
        } catch (IOException e) {
            throw new IllegalStateException("Exception while downloading client mappings", e);
        }
        try {
            serverDL.execute();
        } catch (IOException e) {
            throw new IllegalStateException("Exception while downloading server mappings", e);
        }

        setDidWork(!clientDL.isUpToDate() || !serverDL.isUpToDate());
    }

    private File ensureAvailable(File file) {
        File parent = file.getParentFile();
        if (parent != null) {
            if (parent.exists() && !parent.isDirectory() && !parent.delete()) {
                throw new IllegalStateException("Could not delete " + parent + " to make way for parent directory of " + file.getName());
            }

            if (!parent.exists() && !parent.mkdirs()) {
                throw new IllegalStateException("Could not create parent directory " + parent + " for file " + file.getName());
            }
        }

        if (file.exists() && !file.isDirectory() && !file.delete()) {
            throw new IllegalStateException("Could not delete existing file " + file);
        }

        return file;
    }
}
