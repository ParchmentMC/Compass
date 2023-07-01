package org.parchmentmc.compass;

import com.google.common.collect.ImmutableSet;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.file.RegularFile;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.parchmentmc.compass.storage.io.MappingIOFormat;
import org.parchmentmc.compass.tasks.CopyData;
import org.parchmentmc.compass.tasks.DownloadVersionManifest;
import org.parchmentmc.compass.tasks.JAMMERExec;
import org.parchmentmc.compass.tasks.VersionDownload;
import org.parchmentmc.compass.util.JSONUtil;
import org.parchmentmc.compass.util.download.BlackstoneDownloader;
import org.parchmentmc.feather.manifests.LauncherManifest;
import org.parchmentmc.feather.manifests.LauncherManifest.VersionData;
import org.parchmentmc.feather.manifests.VersionManifest;

import javax.inject.Inject;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class MigrationConfiguration {
    private static final Logger LOGGER = Logging.getLogger(MigrationConfiguration.class);
    public static final String JAMMER_CONFIGURATION_NAME = "jammer";

    private final CompassExtension extension;

    @Inject
    public MigrationConfiguration(final CompassExtension extension) {
        this.extension = extension;

        getTargetVersion().finalizeValueOnRead();
        getExcludedVersions().finalizeValueOnRead();

        // By default, exclude the April Fools versions
        // Note that Minecraft 2.0, despite being an April Fools version, was never published to the launcher
        getExcludedVersions().convention(ImmutableSet.of(
                "15w14a",
                "1.RV-Pre1",
                "3D Shareware v1.34",
                "20w14infinite",
                "22w13oneblockatatime",
                "23w13a_or_b"
        ));
    }

    public abstract Property<String> getTargetVersion();

    public abstract SetProperty<String> getExcludedVersions();

    // Below here lies implementation details

    void setup(final Project project, final CompassPlugin plugin) {
        project.getConfigurations().create(JAMMER_CONFIGURATION_NAME, c -> {
            c.setCanBeResolved(true);
            c.setCanBeConsumed(false);
            c.setDescription("Configuration for the JAMMER (JarAwareMapping) data migration tool.");
        });

        project.afterEvaluate(p -> {
            final @Nullable String targetVersion = extension.getMigration().getTargetVersion().getOrNull();
            if (targetVersion != null) {
                final Set<String> excludedVersions = extension.getMigration().getExcludedVersions().get();
                setupMigration(project, plugin, extension, targetVersion, excludedVersions);
            }
        });
    }

    private static void setupMigration(final Project project, final CompassPlugin plugin, final CompassExtension extension,
                                       final String targetVersion, final Collection<String> excludedVersions) {
        //
        // Determine the versions to be passed to JAMMER
        //

        final String currentVersion = extension.getVersion().get();
        final LauncherManifest launcherManifest = plugin.getManifestsDownloader().getLauncherManifest().get();

        final List<String> versionIds = launcherManifest.getVersions().stream().map(VersionData::getId).collect(Collectors.toList());

        // Just in case, verify the configured excluded versions do exist, and warn if not
        for (String excludedVersion : excludedVersions) {
            if (!versionIds.contains(excludedVersion)) {
                LOGGER.warn("Excluded version {} does not exist in launcher manifest", excludedVersion);
            }
        }

        final int targetVersionIndex = versionIds.indexOf(targetVersion);
        if (targetVersionIndex == -1)
            throw new InvalidUserDataException("Migration target version " + targetVersion + " does not exist in launcher manifest");
        final int currentVersionIndex = versionIds.indexOf(currentVersion);
        assert currentVersionIndex != -1;

        if (targetVersionIndex == currentVersionIndex)
            throw new InvalidUserDataException("Migration target version " + targetVersion + " is the same as the current version");

        final int firstVersion = Math.min(targetVersionIndex, currentVersionIndex);
        final int lastVersion = Math.max(targetVersionIndex, currentVersionIndex);

        final List<VersionData> versions = new ArrayList<>(launcherManifest.getVersions().subList(firstVersion, lastVersion + 1));
        if (firstVersion == targetVersionIndex) {
            // The first version in the list is the target version, therefore we reverse the list so the last version in
            // the list is the target version
            Collections.reverse(versions);
        }
        versions.removeIf(ver -> excludedVersions.contains(ver.getId()));
        final Deque<VersionData> versionsToProcess = new ArrayDeque<>(versions);

        LOGGER.lifecycle("Setting up migration for the following versions in order: {}",
                versionsToProcess.stream().map(VersionData::getId).collect(Collectors.joining(" -> ")));

        // 
        // Setup the tasks
        //

        // For task dependency inference to work, the return value of a file property getter (i.e. `Provider<RegularFile> getOutput()`)
        // *MUST* be an instance of `Property`, even if it's only exposed to API as a `Provider`. It kinda makes sense --
        // since the `Property` instance will be configured at creation by Gradle's magic to know it's connected to a task,
        // which it probably can't do to an arbitrary `Provider`.

        final TaskContainer tasks = project.getTasks();
        project.getConfigurations().maybeCreate(JAMMER_CONFIGURATION_NAME);

        final TaskProvider<CopyData> copyDataForMigration = tasks.register("copyDataForMigration", CopyData.class,
                task -> {
                    task.getInputDirectory().set(extension.getProductionData());
                    task.getInputFormat().set(extension.getProductionDataFormat());
                    task.getOutputFile().set(project.getLayout().getBuildDirectory().dir(task.getName()).map(d -> d.file("production.json")));
                    task.getOutputFormat().set(MappingIOFormat.MDC_SINGLE);
                });

        TaskProvider<JAMMERExec> migrationTask;
        Provider<RegularFile> identifiers = copyDataForMigration.flatMap(CopyData::getOutputFile);
        VersionData previous = versionsToProcess.removeFirst();
        VersionJammerData previousData = createVersionTasks(project, previous);
        do {
            final VersionData current = versionsToProcess.removeFirst();
            final VersionJammerData currentData = createVersionTasks(project, current);

            migrationTask = setupMigration(project,
                    previous,
                    previousData,
                    identifiers,
                    current,
                    currentData);
            identifiers = migrationTask.flatMap(JAMMERExec::getOutputMapping);

            previous = current;
        } while (!versionsToProcess.isEmpty());

        // After processing, identifiers will contain the last output of the last migration task
        final Provider<RegularFile> migratedData = identifiers;

        final TaskProvider<CopyData> writeMigratedData = tasks.register("writeMigratedData", CopyData.class,
                task -> {
                    task.getInputFile().set(migratedData);
                    task.getInputFormat().set(MappingIOFormat.MDC_SINGLE);
                    task.getOutputDirectory().set(extension.getStagingData());
                    task.getOutputFormat().set(extension.getStagingDataFormat());
                });

        project.getTasks().register("migrateData", Task.class, task -> {
            task.setGroup("parchment");
            task.setDescription("Migrates the mapping data from the current version to a target version.");
            task.dependsOn(writeMigratedData);
        });
    }

    private static TaskProvider<JAMMERExec> setupMigration(final Project project,
                                                           final VersionData input,
                                                           final VersionJammerData inputData,
                                                           final Provider<RegularFile> inputIdentifiers,
                                                           final VersionData output,
                                                           final VersionJammerData outputData) {

        return project.getTasks().register(name("migrate", input.getId(), "to", output.getId(), "mappings"),
                JAMMERExec.class, task -> {

                    task.classpath(project.getConfigurations().named(JAMMER_CONFIGURATION_NAME));

                    task.getOutputDirectory()
                            .set(project.getLayout().getBuildDirectory().dir("migrations").map(d -> d.dir(task.getName())));

                    task.getExistingVersions().register(input.getId(), ver -> {
                        ver.getJar().set(inputData.clientJar);
                        ver.getMappings().set(inputData.clientMappings);
                        ver.getMetadata().set(inputData.blackstone);
                        ver.getIdentifiers().set(inputIdentifiers);
                    });

                    task.targetVersion(output.getId(), ver -> {
                        ver.getJar().set(outputData.clientJar);
                        ver.getMappings().set(outputData.clientMappings);
                        ver.getMetadata().set(outputData.blackstone);
                    });
                });
    }

    private static VersionJammerData createVersionTasks(final Project project, final VersionData version) {
        final TaskContainer tasks = project.getTasks();
        final String versionId = version.getId();

        // Version manifest
        final TaskProvider<DownloadVersionManifest> downloadManifest = tasks.register(name("download", versionId, "manifest"),
                DownloadVersionManifest.class, task -> task.getVersion().set(versionId));

        final Provider<VersionManifest> versionManifest = downloadManifest.flatMap(DownloadVersionManifest::getVersionManifest)
                .map(RegularFile::getAsFile)
                .map(JSONUtil::tryParseVersionManifest);

        // Client JAR
        final TaskProvider<VersionDownload> downloadClientJar = tasks.register(name("download", versionId, "client", "jar"),
                VersionDownload.class, task -> {
                    task.getManifest().set(versionManifest);
                    task.getDownloadKey().set("client");
                });

        // Client mappings
        final TaskProvider<VersionDownload> downloadClientMappings = tasks.register(name("download", versionId, "client", "mappings"),
                VersionDownload.class, task -> {
                    task.getManifest().set(versionManifest);
                    task.getDownloadKey().set("client_mappings");
                    task.getFileName().set(versionId + "-client.txt");
                });

        // Blackstone
        final Configuration blackstoneConfiguration = project.getConfigurations().detachedConfiguration(
                project.getDependencies().create(String.format(Locale.ROOT, BlackstoneDownloader.DEFAULT_BLACKSTONE_ARTIFACT_DEPENDENCY, versionId))
        );

        final Provider<RegularFile> blackstone = project.getLayout().file(
                blackstoneConfiguration.getElements().map(s -> s.iterator().next()).map(FileSystemLocation::getAsFile));

        return new VersionJammerData(
                downloadClientJar.flatMap(VersionDownload::getOutputFile),
                downloadClientMappings.flatMap(VersionDownload::getOutputFile),
                blackstone);
    }

    private static class VersionJammerData {
        final Provider<RegularFile> clientJar;
        final Provider<RegularFile> clientMappings;
        final Provider<RegularFile> blackstone;

        private VersionJammerData(Provider<RegularFile> clientJar,
                                  Provider<RegularFile> clientMappings,
                                  Provider<RegularFile> blackstone) {
            this.clientJar = clientJar;
            this.clientMappings = clientMappings;
            this.blackstone = blackstone;
        }
    }

    private static String name(String... names) {
        if (names.length == 0) return "";
        if (names.length == 1) return names[0];

        return Stream.concat(Stream.of(names[0]), Arrays.stream(names).skip(1).map(MigrationConfiguration::capitalize)).collect(Collectors.joining());
    }

    private static String capitalize(String str) {
        if (str.length() <= 1) return str;
        return str.substring(0, 1).toUpperCase(Locale.ROOT) + str.substring(1);
    }
}
