package org.parchmentmc.compass.util.download;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.parchmentmc.compass.util.JSONUtil;
import org.parchmentmc.feather.manifests.LauncherManifest;
import org.parchmentmc.feather.manifests.VersionManifest;

import java.io.File;

import static org.parchmentmc.compass.util.download.DownloadUtil.*;

public class ManifestsDownloader {
    private final Project project;
    private final Property<String> launcherManifestURL;
    private final Property<String> version;
    private final DirectoryProperty outputDirectory;
    @Nullable
    private LauncherManifest launcherManifestData = null;
    @Nullable
    private VersionManifest versionManifestData = null;

    private final Provider<LauncherManifest> launcherManifestProvider;
    private final Provider<VersionManifest> versionManifestProvider;

    public ManifestsDownloader(Project project) {
        this.project = project;
        ObjectFactory objects = project.getObjects();

        outputDirectory = objects.directoryProperty()
                .convention(project.getLayout().getBuildDirectory().dir("manifests"));
        version = objects.property(String.class);
        launcherManifestURL = objects.property(String.class);

        outputDirectory.finalizeValueOnRead();
        version.finalizeValueOnRead();
        launcherManifestURL.finalizeValueOnRead();

        launcherManifestProvider = project.provider(this::downloadLauncherManifest);
        versionManifestProvider = project.provider(this::downloadVersionManifest);
    }

    public Project getProject() {
        return project;
    }

    public Property<String> getLauncherManifestURL() {
        return launcherManifestURL;
    }

    public Property<String> getVersion() {
        return version;
    }

    public DirectoryProperty getManifestsDirectory() {
        return outputDirectory;
    }

    private LauncherManifest downloadLauncherManifest() {
        if (launcherManifestData != null) {
            return launcherManifestData; // We've already executed and downloaded; return cached.
        }

        try {
            String manifestURL = this.launcherManifestURL.get();
            Directory outputDir = this.outputDirectory.get();
            File outputFile = outputDir.file("launcher.json").getAsFile();

            createAndExecuteAction(project, manifestURL, outputFile, "launcher manifest");

            launcherManifestData = JSONUtil.parseLauncherManifest(outputFile.toPath());
        } catch (Exception e) {
            throw new RuntimeException("Failed to download and load launcher manifest", e);
        }

        return launcherManifestData;
    }

    private VersionManifest downloadVersionManifest() {
        if (versionManifestData != null) {
            return versionManifestData; // We've already executed and downloaded; return cached.
        }

        try {
            String version = this.version.get();
            LauncherManifest launcherManifest = downloadLauncherManifest();
            LauncherManifest.VersionData versionData = launcherManifest.getVersions().stream()
                    .filter(str -> str.getId().equals(version))
                    .findFirst()
                    .orElseThrow(() -> new InvalidUserDataException("No version data found for " + version));

            Directory outputDir = this.outputDirectory.get();
            File outputFile = outputDir.file(version + ".json").getAsFile();

            // Only download if our expected hash (from the launcher manifest) is different from our on-disk file
            if (!outputFile.exists() || areNotChecksumsEqual(outputFile, versionData.getSHA1())) {
                createAndExecuteAction(project, versionData.getUrl(), outputFile, "version manifest");
                verifyChecksum(outputFile, versionData.getSHA1(), "version manifest");
            }

            versionManifestData = JSONUtil.parseVersionManifest(outputFile.toPath());
        } catch (Exception e) {
            throw new RuntimeException("Failed to download and load version manifest", e);
        }

        return versionManifestData;
    }

    public Provider<LauncherManifest> getLauncherManifest() {
        return launcherManifestProvider;
    }

    public Provider<VersionManifest> getVersionManifest() {
        return versionManifestProvider;
    }
}
