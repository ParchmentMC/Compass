package org.parchmentmc.compass.tasks;

import de.undercouch.gradle.tasks.download.DownloadAction;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.internal.Pair;
import org.gradle.internal.hash.HashUtil;
import org.parchmentmc.compass.manifest.VersionManifest;
import org.parchmentmc.compass.util.JSONUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class DownloadObfuscationMaps extends BatchedDownload {
    @Internal
    public abstract DirectoryProperty getVersionStorage();

    @Input
    public abstract SetProperty<String> getVersions();

    @Input
    public abstract Property<String> getVersionManifestFilename();

    @Input
    public abstract Property<String> getClientMappingsKey();

    @Input
    public abstract Property<String> getServerMappingsKey();

    @Input
    public abstract Property<String> getClientMappingsFileName();

    @Input
    public abstract Property<String> getServerMappingsFileName();

    public DownloadObfuscationMaps() {
        // See VersionManifest#downloads
        getClientMappingsKey().convention("client_mappings");
        getServerMappingsKey().convention("server_mappings");
        getClientMappingsFileName().convention("mojmap/client.txt");
        getServerMappingsFileName().convention("mojmap/server.txt");

        getOutputs().upToDateWhen(t -> {
            Directory dir = getVersionStorage().get();
            Set<String> versions = getVersions().get();
            String manifestFileName = getVersionManifestFilename().get();
            String clientKey = getClientMappingsKey().get();
            String serverKey = getServerMappingsKey().get();
            String clientFileName = getClientMappingsFileName().get();
            String serverFileName = getServerMappingsFileName().get();

            for (String version : versions) {
                Directory versionDir = dir.dir(version);
                File manifestFile = versionDir.file(manifestFileName).getAsFile();
                if (!manifestFile.exists())
                    return false; // Manifest does not exist

                VersionManifest manifest = JSONUtil.tryParseVersionManifest(manifestFile);
                if (manifest == null)
                    return false; // Manifest could not be parsed

                VersionManifest.DownloadInfo clientDL = manifest.downloads.get(clientKey);
                VersionManifest.DownloadInfo serverDL = manifest.downloads.get(serverKey);
                if (clientDL == null || serverDL == null)
                    return false; // Client or server obf map doesn't exist in manifest

                File clientFile = versionDir.file(clientFileName).getAsFile();
                File serverFile = versionDir.file(serverFileName).getAsFile();
                if (!clientFile.exists() || !serverFile.exists())
                    return false; // Client or server obf map doesn't exist on disk

                if (!HashUtil.sha1(clientFile).asZeroPaddedHexString(40).equals(clientDL.sha1))
                    return false; // Client obf map does not match SHA-1
                if (!HashUtil.sha1(serverFile).asZeroPaddedHexString(40).equals(serverDL.sha1))
                    return false; // Server obf map does not match SHA-1
            }

            return true;
        });
    }

    @Internal
    @Override
    public List<Pair<String, DownloadAction>> getDownloadActions() {
        Directory dir = getVersionStorage().get();
        Set<String> versions = getVersions().get();
        String manifestFileName = getVersionManifestFilename().get();
        String clientKey = getClientMappingsKey().get();
        String serverKey = getServerMappingsKey().get();
        String clientFileName = getClientMappingsFileName().get();
        String serverFileName = getServerMappingsFileName().get();

        List<Pair<String, DownloadAction>> downloadActions = new ArrayList<>();

        for (String version : versions) {
            Directory versionDir = dir.dir(version);
            File manifestFile = versionDir.file(manifestFileName).getAsFile();
            if (!manifestFile.exists())
                throw new IllegalStateException("Manifest file for version " + version + " does not exist");

            VersionManifest manifest = JSONUtil.tryParseVersionManifest(manifestFile);
            if (manifest == null)
                throw new IllegalStateException("Could not parse manifest file for version " + version);

            VersionManifest.DownloadInfo clientDL = manifest.downloads.get(clientKey);
            VersionManifest.DownloadInfo serverDL = manifest.downloads.get(serverKey);
            if (clientDL == null)
                throw new IllegalStateException("Client obfuscation map does not exist in manifest for version " + version);
            if (serverDL == null)
                throw new IllegalStateException("Server obfuscation map does not exist in manifest for version " + version);

            File clientFile = versionDir.file(clientFileName).getAsFile();
            File serverFile = versionDir.file(serverFileName).getAsFile();
            if (!clientFile.exists() || !HashUtil.sha1(clientFile).asZeroPaddedHexString(40).equals(clientDL.sha1)) {
                downloadActions.add(Pair.of("client obfuscation map for " + version,
                        createDownloadAction(clientDL.url, clientFile)));
            }
            if (!serverFile.exists() || !HashUtil.sha1(serverFile).asZeroPaddedHexString(40).equals(serverDL.sha1)) {
                downloadActions.add(Pair.of("server obfuscation map for " + version,
                        createDownloadAction(serverDL.url, serverFile)));
            }
        }

        return downloadActions;
    }
}
