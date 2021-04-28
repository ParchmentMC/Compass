package org.parchmentmc.compass.tasks;

import de.undercouch.gradle.tasks.download.DownloadAction;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.hash.HashUtil;
import org.parchmentmc.compass.manifest.LauncherManifest;

import java.io.File;
import java.io.IOException;

public abstract class DownloadVersionManifest extends DefaultTask {
    @Input
    public abstract Property<String> getVersion();

    @Input
    public abstract Property<LauncherManifest.VersionData> getVersionData();

    @OutputFile
    public abstract RegularFileProperty getOutput();

    public DownloadVersionManifest() {
        getOutput().convention(getProject().getLayout().getBuildDirectory().dir(getName()).map(d -> d.file("manifest.json")));

        getOutputs().upToDateWhen(t -> {
            Provider<String> outputHash = getProject().getProviders().fileContents(getOutput())
                    .getAsBytes()
                    .map(s -> HashUtil.sha1(s).asZeroPaddedHexString(40));
            Provider<String> expectedHash = getVersionData().map(s -> s.sha1);
            return outputHash.isPresent() && outputHash.get().equals(expectedHash.get());
        });
    }

    @TaskAction
    public void download() throws IOException {
        LauncherManifest.VersionData data = getVersionData().get();
        File output = getOutput().get().getAsFile();

        DownloadAction action = new DownloadAction(getProject());
        action.quiet(true);
        action.overwrite(true); // Always check
        action.onlyIfModified(true); // Only re-download if changed
        action.useETag(true); // Use ETag to additionally check for changes
        action.src(data.url);
        action.dest(output);
        try {
            action.execute();
        } catch (IOException e) {
            throw new IOException("Exception while downloading version manfiest for " + getVersion().get(), e);
        }

        setDidWork(!action.isUpToDate());
        if (!getDidWork()) return;

        String expected = data.sha1;
        String actual = HashUtil.sha1(output).asZeroPaddedHexString(40);
        if (!expected.equals(actual)) {
            throw new IOException("Hash for downloaded version manfiest for " + getVersion().get() +
                    " does not match expected; expected " + expected + ", actual is " + actual);
        }
    }
}
