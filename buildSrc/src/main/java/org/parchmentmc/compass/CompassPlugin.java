package org.parchmentmc.compass;

import net.minecraftforge.srgutils.IMappingFile;
import okio.BufferedSink;
import okio.Okio;
import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.parchmentmc.compass.storage.MappingDataContainer;
import org.parchmentmc.compass.storage.io.ExplodedDataIO;
import org.parchmentmc.compass.tasks.DisplayMinecraftVersions;
import org.parchmentmc.compass.util.JSONUtil;
import org.parchmentmc.compass.util.MappingUtil;
import org.parchmentmc.compass.util.download.ManifestsDownloader;
import org.parchmentmc.compass.util.download.ObfuscationMapsDownloader;

import java.io.IOException;
import java.nio.file.Path;

public class CompassPlugin implements Plugin<Project> {
    public static final String COMPASS_GROUP = "compass";

    @Override
    public void apply(Project project) {
        project.getPlugins().apply("de.undercouch.download");
        final CompassExtension extension = project.getExtensions().create("compass", CompassExtension.class, project);
        final TaskContainer tasks = project.getTasks();

        final ManifestsDownloader manifests = new ManifestsDownloader(project);
        manifests.getLauncherManifestURL().set(extension.getLauncherManifestURL());
        manifests.getVersion().set(extension.getVersion());

        ObfuscationMapsDownloader obfuscationMaps = new ObfuscationMapsDownloader(project);
        obfuscationMaps.getVersionManifest().set(manifests.getVersionManifest());

        final TaskProvider<DisplayMinecraftVersions> displayMinecraftVersions = tasks.register("displayMinecraftVersions", DisplayMinecraftVersions.class);
        displayMinecraftVersions.configure(t -> {
            t.setGroup(COMPASS_GROUP);
            t.setDescription("Displays all known Minecraft versions.");
            t.getManifest().set(manifests.getLauncherManifest());
        });

        DefaultTask writeExploded = tasks.create("writeExploded", DefaultTask.class);
        writeExploded.setGroup(COMPASS_GROUP);
        writeExploded.setDescription("temporary task; Writes out the combined obfuscation maps into exploded directories.");
        writeExploded.doLast(t -> {
            Provider<IMappingFile> obfMapProvider = obfuscationMaps.getObfuscationMap();
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
