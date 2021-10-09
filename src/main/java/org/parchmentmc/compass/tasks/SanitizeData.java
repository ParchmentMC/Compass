package org.parchmentmc.compass.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectList;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.parchmentmc.compass.CompassPlugin;
import org.parchmentmc.compass.data.sanitation.DataSanitizer;
import org.parchmentmc.compass.data.sanitation.Sanitizer;
import org.parchmentmc.compass.data.sanitation.impl.BouncerDataMover;
import org.parchmentmc.compass.data.sanitation.impl.DescriptorParametersSanitizer;
import org.parchmentmc.compass.data.sanitation.impl.EnumValueOfRemover;
import org.parchmentmc.compass.data.sanitation.impl.SyntheticsRemover;
import org.parchmentmc.compass.storage.io.MappingIOFormat;
import org.parchmentmc.compass.util.download.BlackstoneDownloader;
import org.parchmentmc.feather.mapping.MappingDataContainer;
import org.parchmentmc.feather.metadata.SourceMetadata;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

public abstract class SanitizeData extends DefaultTask {
    private final NamedDomainObjectList<Sanitizer> sanitizers;

    @InputDirectory
    public abstract DirectoryProperty getInput();

    @Input
    public abstract Property<MappingIOFormat> getInputFormat();

    @Input
    public NamedDomainObjectList<Sanitizer> getSanitizers() {
        return sanitizers;
    }

    @Inject
    public SanitizeData(ObjectFactory objectFactory) {
        sanitizers = objectFactory.namedDomainObjectList(Sanitizer.class);

        getSanitizers().add(new BouncerDataMover());
        getSanitizers().add(new EnumValueOfRemover());
        getSanitizers().add(new SyntheticsRemover());
        getSanitizers().add(new DescriptorParametersSanitizer());
    }

    @TaskAction
    public void sanitize() throws IOException {
        final File input = getInput().get().getAsFile();
        final Logger logger = getProject().getLogger();

        final CompassPlugin plugin = getProject().getPlugins().getPlugin(CompassPlugin.class);
        final BlackstoneDownloader blackstoneDownloader = plugin.getBlackstoneDownloader();

        final SourceMetadata metadata = blackstoneDownloader.retrieveMetadata();

        final MappingDataContainer inputData = getInputFormat().get().read(input);

        DataSanitizer sanitizer = new DataSanitizer();

        getSanitizers().forEach(sanitizer::addSanitizer);

        logger.lifecycle("Sanitizers in use: {}", sanitizer.getSanitizers().stream().map(Named::getName).collect(Collectors.toList()));
        if (metadata == null) {
            logger.warn("No Blackstone metadata loaded, sanitation may not have any effects");
        }
        logger.lifecycle("Sanitizing mapping data in '{}'", input.getAbsolutePath());
        final MappingDataContainer sanitizedData = sanitizer.sanitize(inputData, metadata);

        getInputFormat().get().write(sanitizedData, input);
    }
}
