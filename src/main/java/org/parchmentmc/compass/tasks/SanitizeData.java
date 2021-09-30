package org.parchmentmc.compass.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.parchmentmc.compass.CompassPlugin;
import org.parchmentmc.compass.sanitation.DataSanitizer;
import org.parchmentmc.compass.sanitation.impl.BouncerDataMover;
import org.parchmentmc.compass.sanitation.impl.DescriptorParametersSanitizer;
import org.parchmentmc.compass.sanitation.impl.EnumValueOfRemover;
import org.parchmentmc.compass.sanitation.impl.SyntheticsRemover;
import org.parchmentmc.compass.storage.io.MappingIOFormat;
import org.parchmentmc.compass.util.download.BlackstoneDownloader;
import org.parchmentmc.feather.mapping.MappingDataContainer;
import org.parchmentmc.feather.metadata.SourceMetadata;

import java.io.File;
import java.io.IOException;

public abstract class SanitizeData extends DefaultTask {
    @InputDirectory
    public abstract DirectoryProperty getInput();

    @Input
    public abstract Property<MappingIOFormat> getInputFormat();

    @TaskAction
    public void sanitize() throws IOException {
        final File input = getInput().get().getAsFile();
        final Logger logger = getProject().getLogger();

        final CompassPlugin plugin = getProject().getPlugins().getPlugin(CompassPlugin.class);
        final BlackstoneDownloader blackstoneDownloader = plugin.getBlackstoneDownloader();

        final SourceMetadata metadata = blackstoneDownloader.retrieveMetadata();
        if (metadata == null) {
            logger.warn("No Blackstone metadata loaded, sanitization may not have any effects");
        }

        final MappingDataContainer inputData = getInputFormat().get().read(input);

        DataSanitizer sanitizer = new DataSanitizer();

        sanitizer.addSanitizer(new BouncerDataMover());
        sanitizer.addSanitizer(new EnumValueOfRemover());
        sanitizer.addSanitizer(new SyntheticsRemover());
        sanitizer.addSanitizer(new DescriptorParametersSanitizer());

        final MappingDataContainer sanitizedData = sanitizer.sanitize(inputData, metadata);

        getInputFormat().get().write(sanitizedData, input);
    }
}
