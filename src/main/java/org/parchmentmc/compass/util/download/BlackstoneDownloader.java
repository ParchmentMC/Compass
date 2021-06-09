package org.parchmentmc.compass.util.download;

import com.squareup.moshi.Moshi;
import okio.BufferedSource;
import okio.Okio;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.logging.Logger;
import org.parchmentmc.compass.CompassExtension;
import org.parchmentmc.compass.util.JSONUtil;
import org.parchmentmc.feather.metadata.SourceMetadata;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class BlackstoneDownloader {
    private static final Moshi MOSHI = JSONUtil.MOSHI;
    public static final String BLACKSTONE_CONFIGURATION_NAME = "blackstone";
    public static final String DEFAULT_BLACKSTONE_ARTIFACT_DEPENDENCY = "org.parchmentmc.data:blackstone:%s@zip";
    private static final String JSON_DATA_FILE_NAME = "merged.json";

    private final Project project;

    public BlackstoneDownloader(Project project) {
        this.project = project;
        CompassExtension extension = project.getExtensions().getByType(CompassExtension.class);

        final NamedDomainObjectProvider<Configuration> configuration = project.getConfigurations().register(BLACKSTONE_CONFIGURATION_NAME);
        configuration.configure(c -> {
            c.setCanBeResolved(true);
            c.setCanBeConsumed(false);
            c.setDescription("Configuration for the Blackstone metadata artifact.");
            c.defaultDependencies(d -> d.add(project.getDependencies().create(String.format(DEFAULT_BLACKSTONE_ARTIFACT_DEPENDENCY, extension.getVersion().get()))));
        });
    }

    public Project getProject() {
        return project;
    }

    @Nullable
    private File downloadArtifact() {
        final Logger logger = project.getLogger();

        Configuration mcpConfig = project.getConfigurations().getByName(BLACKSTONE_CONFIGURATION_NAME);
        Set<File> files = mcpConfig.resolve();
        if (files.isEmpty()) {
            return null;
        }

        File file = files.iterator().next();
        if (files.size() > 1) {
            logger.warn("More than 1 artifact in '{}' configuration, using topmost artifact: {}", BLACKSTONE_CONFIGURATION_NAME, file);
        } else {
            logger.debug("Resolved artifact for '{}' configuration: {}", BLACKSTONE_CONFIGURATION_NAME, file);
        }

        return file;
    }

    private boolean cached = false;
    private SourceMetadata cachedData = null;

    @Nullable
    public SourceMetadata retrieveMetadata() throws IOException {
        if (cached) {
            return cachedData;
        }
        cached = true;

        final File artifact = downloadArtifact();
        if (artifact == null) {
            return null;
        }

        try (ZipFile zip = new ZipFile(artifact)) {
            ZipEntry entry = zip.getEntry(JSON_DATA_FILE_NAME);
            if (entry == null && zip.size() == 1) { // If there's only one entry, then that's our json
                entry = zip.entries().nextElement();
            }
            if (entry == null) { // No entry, return null
                return null;
            }

            try (BufferedSource source = Okio.buffer(Okio.source(zip.getInputStream(entry)))) {
                cachedData = MOSHI.adapter(SourceMetadata.class).fromJson(source);
            }
        }

        return cachedData;
    }
}
