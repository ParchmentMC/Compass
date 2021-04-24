package org.parchmentmc.compass.tasks;

import de.undercouch.gradle.tasks.download.Download;
import de.undercouch.gradle.tasks.download.VerifyAction;
import org.parchmentmc.compass.manifest.LauncherManifest;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.internal.FileUtils;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class DownloadVersionManifests extends Download {
    @Input
    public abstract Property<LauncherManifest> getManifest();

    @Input
    public abstract SetProperty<String> getVersions();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    private final Provider<Set<LauncherManifest.VersionData>> versionData;

    public DownloadVersionManifests() {
        versionData = getManifest().zip(getVersions(), (manifest, versions) ->
                versions.stream()
                        .map(ver -> manifest.versions.parallelStream()
                                .filter(v -> v.id.equals(ver))
                                .findFirst()
                                .orElseThrow(() -> new InvalidUserDataException("Could not find version " + ver + " in manifest")))
                        .collect(Collectors.toSet())
        );
        getOutputDirectory().convention(getProject().getLayout().getBuildDirectory().dir(getName()));

        useETag(true);
        onlyIfModified(true);
        src(versionData.map(t -> t.stream().map(s -> s.url).collect(Collectors.toSet())));
        dest(getProject().provider(() -> { // TODO: move to DownloadAction
            Set<LauncherManifest.VersionData> versions = this.versionData.get();
            if (versions.size() == 1) {
                return getOutputDirectory().file(versions.iterator().next().id + ".json").get().getAsFile();
            }
            return getOutputDirectory().get().getAsFile();
        }));
    }

    @Override
    public void download() throws IOException {
        Set<LauncherManifest.VersionData> versions = this.versionData.get();

        super.download();

        // Don't do post-validation if we didn't do anything
        if (!getDidWork()) return;

        Map<String, LauncherManifest.VersionData> versionData = versions.stream()
                .collect(Collectors.toMap(t -> t.id, Function.identity()));

        for (File outputFile : getOutputFiles()) {
            String filename = FileUtils.removeExtension(outputFile.getName());
            LauncherManifest.VersionData data = versionData.get(filename);
            if (data != null) {
                VerifyAction verify = new VerifyAction(getProject());
                verify.src(outputFile);
                verify.algorithm("SHA-1");
                verify.checksum(data.sha1);
                try {
                    verify.execute();
                } catch (IOException e) {
                    getLogger().warn("Could not verify checksum of file " + outputFile.getAbsolutePath() + " matches expected", e);
                } catch (NoSuchAlgorithmException e) { // MessageDigest javadoc states that any JRE must support SHA-1
                    throw new AssertionError("SHA-1 is not a valid algorithm", e);
                }
            } else {
                getLogger().warn("Could not find version data entry for " + outputFile.getAbsolutePath());
            }
        }
    }
}
