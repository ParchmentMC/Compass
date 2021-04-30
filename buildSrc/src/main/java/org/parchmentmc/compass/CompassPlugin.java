package org.parchmentmc.compass;

import de.undercouch.gradle.tasks.download.Download;
import net.minecraftforge.srgutils.IMappingFile;
import okio.BufferedSink;
import okio.Okio;
import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFile;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.parchmentmc.compass.manifest.LauncherManifest;
import org.parchmentmc.compass.manifest.VersionManifest;
import org.parchmentmc.compass.storage.MappingDataContainer;
import org.parchmentmc.compass.storage.io.ExplodedDataIO;
import org.parchmentmc.compass.tasks.DisplayMinecraftVersions;
import org.parchmentmc.compass.tasks.DownloadObfuscationMaps;
import org.parchmentmc.compass.tasks.DownloadVersionManifest;
import org.parchmentmc.compass.util.JSONUtil;
import org.parchmentmc.compass.util.MappingUtil;

import java.io.IOException;
import java.nio.file.Path;

public class CompassPlugin implements Plugin<Project> {
    public static final String COMPASS_GROUP = "compass";

    @Override
    public void apply(Project project) {
        project.getPlugins().apply("de.undercouch.download");
        final CompassExtension extension = project.getExtensions().create("compass", CompassExtension.class, project);
        final TaskContainer tasks = project.getTasks();

        final TaskProvider<Download> downloadLauncherManifest = tasks.register("downloadLauncherManifest", Download.class);
        downloadLauncherManifest.configure(t -> {
            t.setGroup(COMPASS_GROUP);
            t.setDescription("Downloads the launcher manifest.");
            t.src(extension.getLauncherManifestURL());
            t.dest(t.getProject().getLayout().getBuildDirectory().dir(t.getName()).map(d -> d.file("manifest.json").getAsFile()));
            t.overwrite(true);
            t.onlyIfModified(true);
            t.useETag(true);
            t.quiet(true);
        });

        //noinspection NullableProblems
        final Provider<LauncherManifest> launcherManifest = downloadLauncherManifest
                .map(Download::getDest)
                .map(JSONUtil::tryParseLauncherManifest);

        final TaskProvider<DisplayMinecraftVersions> displayMinecraftVersions = tasks.register("displayMinecraftVersions", DisplayMinecraftVersions.class);
        displayMinecraftVersions.configure(t -> {
            t.setGroup(COMPASS_GROUP);
            t.setDescription("Displays all known Minecraft versions.");
            t.getManifest().set(launcherManifest);
        });

        final Provider<LauncherManifest.VersionData> versionData = launcherManifest.zip(extension.getVersion(),
                (mf, ver) -> mf.versions.stream()
                        .filter(str -> str.id.equals(ver))
                        .findFirst()
                        .orElseThrow(() -> new InvalidUserDataException("No version data found for " + ver)));

        TaskProvider<DownloadVersionManifest> downloadVersionManifest = tasks.register("downloadVersionManifest", DownloadVersionManifest.class);
        downloadVersionManifest.configure(t -> {
            t.setGroup(COMPASS_GROUP);
            t.setDescription("Downloads the version manifest.");
            t.getVersionData().set(versionData);
            t.getVersion().set(extension.getVersion());
        });

        //noinspection NullableProblems
        final Provider<VersionManifest> versionManifest = downloadVersionManifest
                .flatMap(DownloadVersionManifest::getOutput)
                .map(RegularFile::getAsFile)
                .map(JSONUtil::tryParseVersionManifest);

        TaskProvider<DownloadObfuscationMaps> downloadObfuscationMaps = tasks.register("downloadObfuscationMaps", DownloadObfuscationMaps.class);
        downloadObfuscationMaps.configure(t -> {
            t.setGroup(COMPASS_GROUP);
            t.setDescription("Downloads the client and server obfuscation maps.");
            t.getVersionManifest().set(versionManifest);
        });

        DefaultTask writeExploded = tasks.create("writeExploded", DefaultTask.class);
        writeExploded.dependsOn(downloadObfuscationMaps);
        writeExploded.setGroup(COMPASS_GROUP);
        writeExploded.setDescription("temporary task; Writes out the combined obfuscation maps into exploded directories.");
        writeExploded.doLast(t -> {
            Provider<IMappingFile> obfMapProvider = downloadObfuscationMaps.flatMap(s -> s.getClientMappingOutput().zip(s.getServerMappingOutput(),
                    (client, server) -> MappingUtil.loadAndEnsureSuperset(client.getAsFile().toPath(), server.getAsFile().toPath())));
            try {
                Path output = extension.getStorageDirectory().get().getAsFile().toPath();
                IMappingFile mojToObf = obfMapProvider.get();

                // getMapped() == obfuscated, getOriginal() == mojmap
                MappingDataContainer obf = MappingUtil.createBuilderFrom(mojToObf, true);
                MappingDataContainer moj = MappingUtil.createBuilderFrom(mojToObf, false);

                Path obfPath = output.resolve("obf");
                Path mojPath = output.resolve("moj");

                ExplodedDataIO.INSTANCE.write(obf, obfPath);
                ExplodedDataIO.INSTANCE.write(moj, mojPath);

                MappingDataContainer readObf = ExplodedDataIO.INSTANCE.read(obfPath);
                MappingDataContainer readMoj = ExplodedDataIO.INSTANCE.read(mojPath);

                try (BufferedSink sink = Okio.buffer(Okio.sink(output.resolve("input_obf.json")))) {
                    JSONUtil.MOSHI.adapter(MappingDataContainer.class).indent("  ").toJson(sink, obf);
                }
                try (BufferedSink sink = Okio.buffer(Okio.sink(output.resolve("input_moj.json")))) {
                    JSONUtil.MOSHI.adapter(MappingDataContainer.class).indent("  ").toJson(sink, moj);
                }

                try (BufferedSink sink = Okio.buffer(Okio.sink(output.resolve("output_obf.json")))) {
                    JSONUtil.MOSHI.adapter(MappingDataContainer.class).indent("  ").toJson(sink, readObf);
                }
                try (BufferedSink sink = Okio.buffer(Okio.sink(output.resolve("output_moj.json")))) {
                    JSONUtil.MOSHI.adapter(MappingDataContainer.class).indent("  ").toJson(sink, readMoj);
                }

                Logger logger = t.getLogger();
                if (obf.equals(readObf)) {
                    logger.lifecycle("Obfuscation: Input mapping data matches read mapping data output");
                } else {
                    logger.warn("Obfuscation: Input mapping data DOES NOT match read mapping data output");
                }
                if (moj.equals(readMoj)) {
                    logger.lifecycle("Mojmaps: Input mapping data matches read mapping data output");
                } else {
                    logger.warn("Mojmaps: Input mapping data DOES NOT match read mapping data output");
                }
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        });
    }
}
