package org.parchmentmc.compass.util.download;

import net.minecraftforge.srgutils.IMappingFile;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.parchmentmc.compass.util.MappingUtil;
import org.parchmentmc.feather.manifests.VersionManifest;

import java.io.File;

import static org.parchmentmc.compass.util.download.DownloadUtil.*;

public class ObfuscationMapsDownloader {
    private final Project project;
    private final Property<VersionManifest> versionManifest;
    private final Property<String> clientDownloadKey;
    private final Property<String> serverDownloadKey;
    private final DirectoryProperty outputDirectory;
    private final Provider<RegularFile> clientDownloadOutput;
    private final Provider<RegularFile> serverDownloadOutput;

    private final Provider<VersionManifest.DownloadInfo> clientDownload;
    private final Provider<VersionManifest.DownloadInfo> serverDownload;
    private final Provider<IMappingFile> obfuscationMapProvider;

    public ObfuscationMapsDownloader(Project project) {
        this.project = project;
        ObjectFactory objects = project.getObjects();

        versionManifest = objects.property(VersionManifest.class);
        // See VersionManifest#downloads
        clientDownloadKey = objects.property(String.class).convention("client_mappings");
        serverDownloadKey = objects.property(String.class).convention("server_mappings");
        outputDirectory = objects.directoryProperty()
                .convention(project.getLayout().getBuildDirectory().dir("obfuscationMaps").zip(versionManifest, (d, manifest) -> d.dir(manifest.getId())));
        clientDownloadOutput = outputDirectory.file("client.txt");
        serverDownloadOutput = outputDirectory.file("server.txt");

        versionManifest.finalizeValueOnRead();
        clientDownloadKey.finalizeValueOnRead();
        serverDownloadKey.finalizeValueOnRead();
        outputDirectory.finalizeValueOnRead();

        clientDownload = versionManifest.zip(clientDownloadKey, (manifest, key) -> {
            VersionManifest.DownloadInfo info = manifest.getDownloads().get(key);
            if (info == null) {
                throw new InvalidUserDataException("No client obfuscation mapping download info for key " + key);
            }
            return info;
        });
        serverDownload = versionManifest.zip(serverDownloadKey, (manifest, key) -> {
            VersionManifest.DownloadInfo info = manifest.getDownloads().get(key);
            if (info == null) {
                throw new InvalidUserDataException("No server obfuscation mapping download info for key " + key);
            }
            return info;
        });

        obfuscationMapProvider = project.provider(this::downloadObfuscationMap);
    }

    public Project getProject() {
        return project;
    }

    public Property<VersionManifest> getVersionManifest() {
        return versionManifest;
    }

    public Property<String> getClientDownloadKey() {
        return clientDownloadKey;
    }

    public Property<String> getServerDownloadKey() {
        return serverDownloadKey;
    }

    public DirectoryProperty getOutputDirectory() {
        return outputDirectory;
    }

    public Provider<RegularFile> getClientDownloadOutput() {
        return clientDownloadOutput;
    }

    public Provider<RegularFile> getServerDownloadOutput() {
        return serverDownloadOutput;
    }

    private void downloadFile(VersionManifest.DownloadInfo download, File output, String info) {
        try {
            createAndExecuteAction(project, download.getUrl(), output, info);
            verifyChecksum(output, download.getSHA1(), info);
        } catch (Exception e) {
            throw new RuntimeException("Failed to download " + info, e);
        }
    }

    @Nullable
    private IMappingFile obfuscationMap = null;

    private IMappingFile downloadObfuscationMap() {
        if (obfuscationMap != null) {
            return obfuscationMap; // We've already executed and downloaded; return cached.
        }
        String version = this.versionManifest.get().getId();

        File clientMappings = clientDownloadOutput.get().getAsFile();
        VersionManifest.DownloadInfo clientInfo = clientDownload.get();
        if (!clientMappings.exists() || areNotChecksumsEqual(clientMappings, clientInfo.getSHA1())) {
            downloadFile(clientInfo, clientMappings, "client obfuscation map for " + version);
        }

        File serverMappings = serverDownloadOutput.get().getAsFile();
        VersionManifest.DownloadInfo serverInfo = serverDownload.get();
        if (!serverMappings.exists() || areNotChecksumsEqual(serverMappings, serverInfo.getSHA1())) {
            downloadFile(serverInfo, serverMappings, "server obfuscation map for " + version);
        }

        obfuscationMap = MappingUtil.loadAndEnsureSuperset(clientMappings.toPath(), serverMappings.toPath());
        obfuscationMap = MappingUtil.constructPackageData(obfuscationMap);

        return obfuscationMap;
    }

    public Provider<IMappingFile> getObfuscationMap() {
        return obfuscationMapProvider;
    }
}
