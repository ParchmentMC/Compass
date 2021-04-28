package org.parchmentmc.compass.tasks;

import de.undercouch.gradle.tasks.download.DownloadAction;
import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.hash.HashUtil;
import org.parchmentmc.compass.manifest.VersionManifest;

import java.io.File;
import java.io.IOException;

public abstract class DownloadObfuscationMaps extends DefaultTask {
    @Input
    public abstract Property<VersionManifest> getVersionManifest();

    @Input
    public abstract Property<String> getClientMappingDownloadKey();

    @Input
    public abstract Property<String> getServerMappingDownloadKey();

    @Input
    public Provider<VersionManifest.DownloadInfo> getClientMappingDownloadInfo() {
        return getVersionManifest().zip(getClientMappingDownloadKey(), (manifest, key) -> {
            VersionManifest.DownloadInfo info = manifest.downloads.get(key);
            if (info == null) {
                throw new InvalidUserDataException("No client obfuscation mapping download info for key " + key);
            }
            return info;
        });
    }

    @Input
    public Provider<VersionManifest.DownloadInfo> getServerMappingDownloadInfo() {
        return getVersionManifest().zip(getServerMappingDownloadKey(), (manifest, key) -> {
            VersionManifest.DownloadInfo info = manifest.downloads.get(key);
            if (info == null) {
                throw new InvalidUserDataException("No server obfuscation mapping download info for key " + key);
            }
            return info;
        });
    }

    @OutputFile
    public abstract RegularFileProperty getClientMappingOutput();

    @OutputFile
    public abstract RegularFileProperty getServerMappingOutput();

    public DownloadObfuscationMaps() {
        Provider<Directory> outputDirectory = getProject().getLayout().getBuildDirectory().dir(getName());
        getClientMappingOutput().convention(outputDirectory.map(d -> d.file("client.txt")));
        getServerMappingOutput().convention(outputDirectory.map(d -> d.file("server.txt")));
        // See VersionManifest#downloads
        getClientMappingDownloadKey().convention("client_mappings");
        getServerMappingDownloadKey().convention("server_mappings");

        getOutputs().upToDateWhen(t -> {
            Provider<String> serverOutputHash = getProject().getProviders().fileContents(getServerMappingOutput())
                    .getAsBytes()
                    .map(s -> HashUtil.sha1(s).asZeroPaddedHexString(40));
            Provider<String> serverExpectedHash = getServerMappingDownloadInfo().map(s -> s.sha1);

            Provider<String> clientOutputHash = getProject().getProviders().fileContents(getClientMappingOutput())
                    .getAsBytes()
                    .map(s -> HashUtil.sha1(s).asZeroPaddedHexString(40));
            Provider<String> clientExpectedHash = getClientMappingDownloadInfo().map(s -> s.sha1);

            return (serverOutputHash.isPresent() && serverOutputHash.get().equals(serverExpectedHash.get())) &&
                    (clientExpectedHash.isPresent() && clientOutputHash.get().equals(clientExpectedHash.get()));
        });
    }

    @TaskAction
    public void download() throws IOException {
        VersionManifest.DownloadInfo clientInfo = getClientMappingDownloadInfo().get();
        VersionManifest.DownloadInfo serverInfo = getServerMappingDownloadInfo().get();
        File clientOutput = getClientMappingOutput().get().getAsFile();
        File serverOutput = getServerMappingOutput().get().getAsFile();

        DownloadAction clientDL = createAndExecuteAction(clientInfo.url, clientOutput, "client obfuscation map");
        DownloadAction serverDL = createAndExecuteAction(serverInfo.url, serverOutput, "server obfuscation map");

        setDidWork(!clientDL.isUpToDate() || !serverDL.isUpToDate());

        if (!clientDL.isUpToDate()) {
            verifyChecksum(clientOutput, clientInfo.sha1, "client obfuscation map");
        }
        if (!serverDL.isUpToDate()) {
            verifyChecksum(serverOutput, serverInfo.sha1, "server obfuscation map");
        }
    }

    private DownloadAction createAndExecuteAction(Object url, Object output, String info) throws IOException {
        DownloadAction action = new DownloadAction(getProject());
        action.quiet(true);
        action.overwrite(true); // Always check
        action.onlyIfModified(true); // Only re-download if changed
        action.useETag(true); // Use ETag to additionally check for changes
        action.src(url);
        action.dest(output);
        try {
            action.execute();
        } catch (IOException e) {
            throw new IOException("Exception while downloading " + info, e);
        }
        return action;
    }

    private void verifyChecksum(File output, String expected, String info) throws IOException {
        String actual = HashUtil.sha1(output).asZeroPaddedHexString(40);
        if (!expected.equals(actual)) {
            throw new IOException("Hash for downloaded " + info +
                    " does not match expected; expected " + expected + ", actual is " + actual);
        }
    }
}
