package org.parchmentmc.compass;

import net.minecraftforge.srgutils.IMappingFile;
import org.gradle.api.DefaultTask;
import org.gradle.api.NamedDomainObjectSet;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.parchmentmc.compass.providers.DelegatingProvider;
import org.parchmentmc.compass.providers.IntermediateProvider;
import org.parchmentmc.compass.providers.mcpconfig.SRGProvider;
import org.parchmentmc.compass.tasks.*;
import org.parchmentmc.compass.util.MappingUtil;
import org.parchmentmc.compass.util.download.BlackstoneDownloader;
import org.parchmentmc.compass.util.download.ManifestsDownloader;
import org.parchmentmc.compass.util.download.ObfuscationMapsDownloader;
import org.parchmentmc.feather.mapping.MappingDataBuilder;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;

public class CompassPlugin implements Plugin<Project> {
    public static final String COMPASS_GROUP = "compass";
    public static final String COMPASS_EXTENSION = "compass";

    public static final String DISPLAY_MINECRAFT_VERSIONS_TASK_NAME = "displayMinecraftVersions";
    public static final String GENERATE_VERSION_BASE_TASK_NAME = "generateVersionBase";
    public static final String CLEAR_STAGING_DATA_TASK_NAME = "clearStaging";
    public static final String PROMOTE_STAGING_DATA_TASK_NAME = "promoteStagingToProduction";
    public static final String CREATE_STAGING_DATA_TASK_NAME = "createStagingFromInputs";
    public static final String SANITIZE_DATA_TASK_NAME = "sanitizeData";
    public static final String SANITIZE_STAGING_DATA_TASK_NAME = "sanitizeStagingData";
    public static final String VALIDATE_DATA_TASK_NAME = "validateData";
    public static final String VALIDATE_STAGING_DATA_TASK_NAME = "validateStagingData";

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
        final CompassExtension extension = project.getExtensions().create(COMPASS_EXTENSION, CompassExtension.class);
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
                MappingDataBuilder data = MappingUtil.loadOfficialData(obfuscationMapsDownloader);

                File stagingDataDir = extension.getStagingData().get().getAsFile();

                try {
                    extension.getStagingDataFormat().get().write(data, stagingDataDir);
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

        createValidationTask(extension, tasks);
        createStagingTasks(extension, tasks);
        createSanitizeTask(extension, tasks);

        intermediates.all(prov -> {
            String capitalized = prov.getName().substring(0, 1).toUpperCase(Locale.ROOT) + prov.getName().substring(1);
            tasks.register("generate" + capitalized + "Export", GenerateExport.class, t -> {
                t.setGroup(COMPASS_GROUP);
                t.setDescription("Generates an export file using the '" + prov.getName() + "' intermediate provider and production data.");
                t.mustRunAfter(tasks.named(PROMOTE_STAGING_DATA_TASK_NAME));
                t.getIntermediate().set(prov.getName());
                t.getInput().set(extension.getProductionData());
                t.getInputFormat().set(extension.getStagingDataFormat());
            });
            tasks.register("generate" + capitalized + "StagingExport", GenerateExport.class, t -> {
                t.setGroup(COMPASS_GROUP);
                t.setDescription("Generates an export file using the '" + prov.getName() + "' intermediate provider and staging data.");
                t.mustRunAfter(tasks.named(CREATE_STAGING_DATA_TASK_NAME));
                t.getIntermediate().set(prov.getName());
                t.getInput().set(extension.getStagingData());
                t.getInputFormat().set(extension.getStagingDataFormat());
            });
        });
    }

    private void createValidationTask(CompassExtension extension, TaskContainer tasks) {
        final TaskProvider<ValidateData> validateData = tasks.register(VALIDATE_DATA_TASK_NAME, ValidateData.class);
        final TaskProvider<ValidateData> validateStagingData = tasks.register(VALIDATE_STAGING_DATA_TASK_NAME, ValidateData.class);

        validateData.configure(t -> {
            t.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
            t.setDescription("Validates the production data.");
            t.mustRunAfter(tasks.named(PROMOTE_STAGING_DATA_TASK_NAME));
            t.getInput().set(extension.getProductionData());
            t.getInputFormat().set(extension.getProductionDataFormat());
        });
        validateStagingData.configure(t -> {
            t.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
            t.setDescription("Validates the staging data.");
            t.mustRunAfter(tasks.named(CREATE_STAGING_DATA_TASK_NAME));
            t.getInput().set(extension.getStagingData());
            t.getInputFormat().set(extension.getStagingDataFormat());
        });
    }

    private void createStagingTasks(CompassExtension extension, TaskContainer tasks) {
        TaskProvider<Delete> clearStaging = tasks.register(CLEAR_STAGING_DATA_TASK_NAME, Delete.class);
        TaskProvider<CopyData> promoteStagingToProduction = tasks.register(PROMOTE_STAGING_DATA_TASK_NAME, CopyData.class);
        TaskProvider<CreateStagingData> createStagingData = tasks.register(CREATE_STAGING_DATA_TASK_NAME, CreateStagingData.class);

        clearStaging.configure(t -> {
            t.setGroup(COMPASS_GROUP);
            t.setDescription("Clears the staging data.");
            t.delete(extension.getStagingData());
        });

        promoteStagingToProduction.configure(t -> {
            t.setGroup(COMPASS_GROUP);
            t.setDescription("Promotes the staging data to production.");
            t.mustRunAfter(tasks.named(CREATE_STAGING_DATA_TASK_NAME));
            t.finalizedBy(clearStaging);
            t.getInput().convention(extension.getStagingData());
            t.getInputFormat().convention(extension.getStagingDataFormat());
            t.getOutput().convention(extension.getProductionData());
            t.getOutputFormat().convention(extension.getProductionDataFormat());
        });

        createStagingData.configure(t -> {
            t.setGroup(COMPASS_GROUP);
            t.setDescription("Combines the input files with the current production data to create the staging data.");
        });
    }

    private void createSanitizeTask(CompassExtension extension, TaskContainer tasks) {
        final TaskProvider<SanitizeData> sanitizeData = tasks.register(SANITIZE_DATA_TASK_NAME, SanitizeData.class);
        final TaskProvider<SanitizeData> sanitizeStagingData = tasks.register(SANITIZE_STAGING_DATA_TASK_NAME, SanitizeData.class);

        sanitizeData.configure(t -> {
            t.setGroup(COMPASS_GROUP);
            t.mustRunAfter(tasks.named(PROMOTE_STAGING_DATA_TASK_NAME));
            t.setDescription("Sanitizes the production data.");
            t.getInput().set(extension.getProductionData());
            t.getInputFormat().set(extension.getProductionDataFormat());
        });
        sanitizeStagingData.configure(t -> {
            t.setGroup(COMPASS_GROUP);
            t.mustRunAfter(tasks.named(CREATE_STAGING_DATA_TASK_NAME));
            t.setDescription("Sanitizes the staging data by removing unnecessary data.");
            t.getInput().set(extension.getStagingData());
            t.getInputFormat().set(extension.getStagingDataFormat());
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
