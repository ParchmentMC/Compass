package org.parchmentmc.compass;

import net.minecraftforge.srgutils.IMappingFile;
import okio.BufferedSink;
import okio.Okio;
import org.gradle.api.DefaultTask;
import org.gradle.api.NamedDomainObjectSet;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.parchmentmc.compass.providers.DelegatingProvider;
import org.parchmentmc.compass.providers.IntermediateProvider;
import org.parchmentmc.compass.providers.mcpconfig.SRGProvider;
import org.parchmentmc.compass.storage.input.InputsReader;
import org.parchmentmc.compass.storage.io.ExplodedDataIO;
import org.parchmentmc.compass.tasks.DisplayMinecraftVersions;
import org.parchmentmc.compass.tasks.GenerateExport;
import org.parchmentmc.compass.tasks.SanitizeStagingData;
import org.parchmentmc.compass.tasks.ValidateMappingData;
import org.parchmentmc.compass.util.JSONUtil;
import org.parchmentmc.compass.util.download.BlackstoneDownloader;
import org.parchmentmc.compass.util.download.ManifestsDownloader;
import org.parchmentmc.compass.util.download.ObfuscationMapsDownloader;
import org.parchmentmc.feather.mapping.MappingDataBuilder;
import org.parchmentmc.feather.mapping.MappingDataContainer;
import org.parchmentmc.feather.mapping.MappingUtil;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;

import static org.parchmentmc.compass.util.MappingUtil.constructPackageData;
import static org.parchmentmc.compass.util.MappingUtil.createBuilderFrom;

public class CompassPlugin implements Plugin<Project> {
    public static final String COMPASS_GROUP = "compass";
    public static final String COMPASS_EXTENSION = "compass";

    public static final String DISPLAY_MINECRAFT_VERSIONS_TASK_NAME = "displayMinecraftVersions";
    public static final String GENERATE_VERSION_BASE_TASK_NAME = "generateVersionBase";
    public static final String CLEAR_STAGING_DATA_TASK_NAME = "clearStaging";
    public static final String PROMOTE_STAGING_DATA_TASK_NAME = "promoteStagingToProduction";
    public static final String CREATE_STAGING_DATA_TASK_NAME = "createStagingFromInputs";
    public static final String SANITIZE_STAGING_DATA_TASK_NAME = "sanitizeStagingData";

    private final NamedDomainObjectSet<IntermediateProvider> intermediates;
    private ManifestsDownloader manifestsDownloader;
    private ObfuscationMapsDownloader obfuscationMapsDownloader;
    private BlackstoneDownloader blackstoneDownloader;

    @Inject
    public CompassPlugin(ObjectFactory objectFactory) {
        this.intermediates = objectFactory.namedDomainObjectSet(IntermediateProvider.class);
    }

    @Override
    public void apply(Project project) {
        project.getPlugins().apply("de.undercouch.download");
        final CompassExtension extension = project.getExtensions().create(COMPASS_EXTENSION, CompassExtension.class, project);
        final TaskContainer tasks = project.getTasks();

        manifestsDownloader = new ManifestsDownloader(project);
        obfuscationMapsDownloader = new ObfuscationMapsDownloader(project);
        blackstoneDownloader = new BlackstoneDownloader(project);

        manifestsDownloader.getLauncherManifestURL().set(extension.getLauncherManifestURL());
        manifestsDownloader.getVersion().set(extension.getVersion());

        obfuscationMapsDownloader.getVersionManifest().set(manifestsDownloader.getVersionManifest());

        final TaskProvider<DisplayMinecraftVersions> displayMinecraftVersions = tasks.register(DISPLAY_MINECRAFT_VERSIONS_TASK_NAME, DisplayMinecraftVersions.class);
        displayMinecraftVersions.configure(t -> {
            t.setGroup(COMPASS_GROUP);
            t.setDescription("Displays all known Minecraft versions.");
            t.getManifest().set(manifestsDownloader.getLauncherManifest());
        });

        TaskProvider<DefaultTask> generateVersionBase = tasks.register(GENERATE_VERSION_BASE_TASK_NAME, DefaultTask.class);
        generateVersionBase.configure(t -> {
            t.setGroup(COMPASS_GROUP);
            t.setDescription("Generates the base data for the active version to the staging directory.");
            t.doLast(_t -> {
                IMappingFile obfMap = obfuscationMapsDownloader.getObfuscationMap().get();
                // reversed because normally, obf map is [Moj -> Obf] (because it's a ProGuard log of the obf)
                MappingDataBuilder data = constructPackageData(createBuilderFrom(obfMap, true));

                File stagingDataDir = extension.getStagingData().get().getAsFile();

                try {
                    ExplodedDataIO.INSTANCE.write(data, stagingDataDir);
                } catch (IOException e) {
                    throw new UncheckedIOException("Failed to write base data for active version to staging directory", e);
                }
            });
        });

        Provider<IMappingFile> obfMapProvider = obfuscationMapsDownloader.getObfuscationMap();
        // noinspection NullableProblems
        Provider<IMappingFile> officialMapProvider = obfMapProvider.map(IMappingFile::reverse);

        intermediates.add(new DelegatingProvider("obf", officialMapProvider.map(s -> s.chain(s.reverse()))));
        intermediates.add(new DelegatingProvider("official", officialMapProvider));
        intermediates.add(new SRGProvider("srg", project));

        intermediates.all(prov -> {
            String capitalized = prov.getName().substring(0, 1).toUpperCase(Locale.ROOT) + prov.getName().substring(1);
            tasks.register("generate" + capitalized + "Export", GenerateExport.class, t -> {
                t.setGroup(COMPASS_GROUP);
                t.setDescription("Generates an export file using the '" + prov.getName() + "' intermediate provider and production data.");
                t.getIntermediate().set(prov.getName());
                t.getInput().set(extension.getProductionData());
            });
            tasks.register("generate" + capitalized + "StagingExport", GenerateExport.class, t -> {
                t.setGroup(COMPASS_GROUP);
                t.setDescription("Generates an export file using the '" + prov.getName() + "' intermediate provider and staging data.");
                t.getIntermediate().set(prov.getName());
                t.getInput().set(extension.getStagingData());
            });
        });

        createValidationTask(extension, tasks);
        createStagingTasks(extension, tasks);
        createSanitizeTask(extension, tasks);

        DefaultTask writeExploded = tasks.create("writeExploded", DefaultTask.class);
        writeExploded.setGroup(COMPASS_GROUP);
        writeExploded.setDescription("temporary task; Writes out the combined obfuscation maps into exploded directories.");
        writeExploded.doLast(t -> {
            try {
                Path output = extension.getProductionData().get().getAsFile().toPath();
                IMappingFile mojToObf = obfMapProvider.get();

                // getMapped() == obfuscated, getOriginal() == mojmap
                MappingDataContainer obf = constructPackageData(createBuilderFrom(mojToObf, true));
                MappingDataContainer moj = constructPackageData(createBuilderFrom(mojToObf, false));

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

    private void createValidationTask(CompassExtension extension, TaskContainer tasks) {
        final TaskProvider<ValidateMappingData> validateData = tasks.register("validateData", ValidateMappingData.class);
        final TaskProvider<ValidateMappingData> validateStagingData = tasks.register("validateStagingData", ValidateMappingData.class);

        validateData.configure(t -> {
            t.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
            t.setDescription("Validates the production data.");
            t.getInput().set(extension.getProductionData());
        });
        validateStagingData.configure(t -> {
            t.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
            t.setDescription("Validates the staging data.");
            t.getInput().set(extension.getStagingData());
        });
    }

    private void createStagingTasks(CompassExtension extension, TaskContainer tasks) {
        TaskProvider<Delete> clearStaging = tasks.register(CLEAR_STAGING_DATA_TASK_NAME, Delete.class);
        TaskProvider<DefaultTask> promoteStagingToProduction = tasks.register(PROMOTE_STAGING_DATA_TASK_NAME, DefaultTask.class);
        TaskProvider<DefaultTask> combineInputData = tasks.register(CREATE_STAGING_DATA_TASK_NAME, DefaultTask.class);

        clearStaging.configure(t -> {
            t.setGroup(COMPASS_GROUP);
            t.setDescription("Clears the staging data.");
            t.delete(extension.getStagingData());
        });

        promoteStagingToProduction.configure(t -> {
            t.setGroup(COMPASS_GROUP);
            t.setDescription("Promotes the staging data to production.");
            t.onlyIf(_t -> extension.getStagingData().get().getAsFile().exists());
            t.doLast(_t -> {
                File stagingDataDir = extension.getStagingData().get().getAsFile();
                if (stagingDataDir.exists()) {
                    String[] list = stagingDataDir.list();
                    if (list != null && list.length == 0) return;
                } else {
                    return;
                }

                MappingDataContainer staging;
                try {
                    staging = ExplodedDataIO.INSTANCE.read(stagingDataDir.toPath());
                } catch (IOException e) {
                    throw new UncheckedIOException("Failed to read staging data for promotion", e);
                }
                try {
                    // noinspection ResultOfMethodCallIgnored
                    Files.walk(stagingDataDir.toPath())
                            .sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                } catch (IOException e) {
                    t.getLogger().warn("Unable to delete staging data directory; continuing", e);
                }

                File prodDataDir = extension.getProductionData().get().getAsFile();
                try {
                    ExplodedDataIO.INSTANCE.write(staging, prodDataDir.toPath());
                } catch (IOException e) {
                    throw new UncheckedIOException("Failed to write promoted staging->production data", e);
                }
            });
        });

        combineInputData.configure(t -> {
            t.setGroup(COMPASS_GROUP);
            t.setDescription("Combines the input files with the current production data to create the staging data.");
            t.doLast(_t -> {
                try {
                    InputsReader inputsReader = new InputsReader(intermediates);

                    MappingDataContainer inputData = inputsReader.parse(extension.getInputs().get().getAsFile().toPath());
                    MappingDataContainer baseData = ExplodedDataIO.INSTANCE.read(extension.getProductionData().get().getAsFile());

                    MappingDataContainer combinedData = MappingUtil.apply(baseData, inputData);

                    ExplodedDataIO.INSTANCE.write(combinedData, extension.getStagingData().get().getAsFile());
                } catch (IOException e) {
                    throw new RuntimeException("Failed to produce new staging data from inputs and production data", e);
                }
            });
        });
    }

    private void createSanitizeTask(CompassExtension extension, TaskContainer tasks) {
        final TaskProvider<SanitizeStagingData> sanitizeStagingData = tasks.register(SANITIZE_STAGING_DATA_TASK_NAME, SanitizeStagingData.class);
        sanitizeStagingData.configure(t -> {
            t.setGroup(COMPASS_GROUP);
            t.setDescription("Sanitizes the staging data by removing unnecessary data.");
            t.getInput().set(extension.getStagingData());
        });
    }

    public NamedDomainObjectSet<IntermediateProvider> getIntermediates() {
        return intermediates;
    }

    public ManifestsDownloader getManifestsDownloader() {
        return manifestsDownloader;
    }

    public ObfuscationMapsDownloader getObfuscationMapsDownloader() {
        return obfuscationMapsDownloader;
    }

    public BlackstoneDownloader getBlackstoneDownloader() {
        return blackstoneDownloader;
    }
}
