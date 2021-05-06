package org.parchmentmc.compass.providers.mcpconfig;

import com.squareup.moshi.Moshi;
import net.minecraftforge.srgutils.IMappingFile;
import okio.BufferedSource;
import okio.Okio;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.logging.Logger;
import org.parchmentmc.compass.CompassExtension;
import org.parchmentmc.compass.providers.IntermediateProvider;
import org.parchmentmc.compass.util.JSONUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Set;

/**
 * Provides the SRG intermediate format, based on an MCPConfig artifact.
 */
public class SRGProvider extends IntermediateProvider {
    private static final Moshi MOSHI = JSONUtil.MOSHI;
    public static final String MCP_CONFIG_CONFIGURATION_NAME = "mcpconfig";
    public static final String DEFAULT_ARTIFACT_DEPENDENCY = "de.oceanlabs.mcp:mcp_config:%s@zip";

    private static final String CONFIG_JSON = "config.json";

    protected final Project project;

    public SRGProvider(String name, Project project) {
        super(name);
        this.project = project;
        CompassExtension extension = project.getExtensions().getByType(CompassExtension.class);

        NamedDomainObjectProvider<Configuration> configuration = project.getConfigurations().register(MCP_CONFIG_CONFIGURATION_NAME);
        configuration.configure(c -> {
            c.setCanBeResolved(true);
            c.defaultDependencies(d -> d.add(project.getDependencies().create(String.format(DEFAULT_ARTIFACT_DEPENDENCY, extension.getVersion().get()))));
            c.setDescription("Configuration for the MCPConfig artifact.");
            c.setVisible(false);
        });
    }

    @Override
    public IMappingFile getMapping() throws IOException {
        final Logger logger = project.getLogger();

        Configuration mcpConfig = project.getConfigurations().getByName(MCP_CONFIG_CONFIGURATION_NAME);
        Set<File> files = mcpConfig.resolve();
        if (files.isEmpty())
            throw new IllegalStateException("No files in '" + MCP_CONFIG_CONFIGURATION_NAME + "' configuration");
        File file = files.iterator().next();
        if (files.size() > 1) {
            logger.warn("More than 1 artifact in '{}' configuration, using topmost artifact: {}", MCP_CONFIG_CONFIGURATION_NAME, file);
        }
        logger.debug("Retrieved artifact for '{}' artifact: {}", MCP_CONFIG_CONFIGURATION_NAME, file);

        CompassExtension extension = project.getExtensions().getByType(CompassExtension.class);

        IMappingFile mapping;

        try (FileSystem fs = FileSystems.newFileSystem(file.toPath(), null)) {
            MCPConfigFile configFile;
            try (BufferedSource source = Okio.buffer(Okio.source(fs.getPath(CONFIG_JSON)))) {
                configFile = MOSHI.adapter(MCPConfigFile.class).fromJson(source);
                if (configFile == null)
                    throw new IOException("Failed to read " + CONFIG_JSON + " from MCPConfig artifact");
            }

            logger.debug("MCPConfig spec version: {}, MC version: {}, mappings file: '{}'", configFile.spec, configFile.version, configFile.data.mappings);
            if (configFile.spec != 1 && configFile.spec != 2) {
                logger.warn("MCPConfig artifact has unrecognized spec of {}", configFile.spec);
            }
            if (!configFile.version.equals(extension.getVersion().get())) {
                logger.warn("MCPConfig artifact's stored version of {} does not match configured version {}", configFile.version, extension.getVersion().get());
            }

            try (InputStream in = Files.newInputStream(fs.getPath(configFile.data.mappings))) {
                mapping = IMappingFile.load(in);
            }

            logger.info("Loaded SRG mappings from MCPConfig artifact: {} classes", mapping.getClasses().size());

            return mapping;
        }
    }
}
