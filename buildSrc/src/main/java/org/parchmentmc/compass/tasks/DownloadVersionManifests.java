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
import org.parchmentmc.compass.manifest.LauncherManifest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class DownloadVersionManifests extends BatchedDownload {
    @Input
    public abstract Property<LauncherManifest> getLauncherManifest();

    @Internal
    public abstract DirectoryProperty getVersionStorage();

    @Input
    public abstract SetProperty<String> getVersions();

    @Input
    public abstract Property<String> getOutputFileName();

    public DownloadVersionManifests() {
        getOutputs().upToDateWhen(t -> {
            Map<String, String> versionsToChecksums = getLauncherManifest().get().versions
                    .stream().collect(Collectors.toMap(v -> v.id, v -> v.sha1));
            Directory dir = getVersionStorage().get();
            Set<String> versions = getVersions().get();
            String filename = getOutputFileName().get();

            for (String version : versions) {
                File manifest = dir.dir(version).file(filename).getAsFile();
                if (!manifest.exists()) return false;
                String expected = versionsToChecksums.get(version);
                if (expected == null) return false;
                if (!HashUtil.sha1(manifest).asZeroPaddedHexString(40).equals(expected)) return false;
            }

            return true;
        });
    }

    @Internal
    @Override
    public List<Pair<String, DownloadAction>> getDownloadActions() {
        LauncherManifest launcherManifest = getLauncherManifest().get();
        String outputFilename = getOutputFileName().get();
        Map<String, LauncherManifest.VersionData> versionsData = launcherManifest.versions.stream().collect(Collectors.toMap(t -> t.id, Function.identity()));
        Directory versionStorage = getVersionStorage().get();
        Set<String> versions = getVersions().get();

        List<Pair<String, DownloadAction>> downloadActions = new ArrayList<>();

        for (String version : versions) {
            LauncherManifest.VersionData versionData = versionsData.get(version);
            if (versionData == null) {
                throw new IllegalStateException("No version data for " + version);
            }
            File versionDir = versionStorage.dir(version).getAsFile();
            if (!versionDir.exists() && !versionDir.mkdirs()) {
                throw new RuntimeException("Could not create version storage directory for " + version);
            }
            File output = new File(versionDir, outputFilename);

            if (!output.exists() || !HashUtil.sha1(output).asZeroPaddedHexString(40).equals(versionData.sha1)) {
                downloadActions.add(Pair.of("version manifest for " + version,
                        createDownloadAction(versionData.url, new File(versionDir, outputFilename))));
            }
        }

        return downloadActions;
    }
}
