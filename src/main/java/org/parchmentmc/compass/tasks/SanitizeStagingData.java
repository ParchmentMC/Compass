package org.parchmentmc.compass.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.parchmentmc.compass.CompassPlugin;
import org.parchmentmc.compass.storage.io.MappingIOFormat;
import org.parchmentmc.compass.util.download.BlackstoneDownloader;
import org.parchmentmc.feather.mapping.MappingDataBuilder;
import org.parchmentmc.feather.mapping.MappingDataContainer;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.FieldMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;
import org.parchmentmc.feather.metadata.SourceMetadata;
import org.parchmentmc.feather.util.AccessFlag;

import java.io.File;
import java.io.IOException;

import static org.parchmentmc.feather.mapping.MappingDataBuilder.*;

public abstract class SanitizeStagingData extends DefaultTask {
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

        final MappingDataContainer data = getInputFormat().get().read(input);

        final MappingDataBuilder builder = new MappingDataBuilder();

        // Packages
        data.getPackages().forEach(t -> builder.createPackage(t.getName()).addJavadoc(t.getJavadoc()));

        // Classes
        for (MappingDataContainer.ClassData clsData : data.getClasses()) {
            final ClassMetadata clsMeta = metadata != null ? metadata.getClasses().stream()
                    .filter(s -> s.getName().getMojangName().orElse("").contentEquals(clsData.getName()))
                    .findFirst().orElse(null) : null;

            final MutableClassData newClsData = builder.getOrCreateClass(clsData.getName())
                    .addJavadoc(clsData.getJavadoc());

            // Fields
            for (MappingDataContainer.FieldData fieldData : clsData.getFields()) {
                final FieldMetadata fieldMeta = clsMeta != null ? clsMeta.getFields().stream()
                        .filter(s -> s.getName().getMojangName().orElse("").contentEquals(fieldData.getName()))
                        .findFirst().orElse(null) : null;

                final MutableFieldData newFieldData = newClsData.getOrCreateField(fieldData.getName(), fieldData.getDescriptor());

                if (!fieldData.getJavadoc().isEmpty() && fieldMeta != null && fieldMeta.hasAccessFlag(AccessFlag.SYNTHETIC)) {
                    logger.lifecycle("Removing javadoc for synthetic field {}.{}", clsData.getName(),
                            fieldData.getName());
                } else {
                    newFieldData.addJavadoc(fieldData.getJavadoc());
                }
            }

            // Methods
            for (MappingDataContainer.MethodData methodData : clsData.getMethods()) {
                final MethodMetadata methodMeta = clsMeta != null ? clsMeta.getMethods().stream()
                        .filter(s -> s.getName().getObfuscatedName().orElse("").contentEquals(methodData.getName())
                                && s.getDescriptor().getObfuscatedName().orElse("").contentEquals(methodData.getDescriptor()))
                        .findFirst().orElse(null) : null;
                final boolean isSynthetic = methodMeta != null && methodMeta.hasAccessFlag(AccessFlag.SYNTHETIC);

                final MutableMethodData newMethodData = newClsData.getOrCreateMethod(methodData.getName(), methodData.getDescriptor());

                if (isSynthetic && !methodData.getJavadoc().isEmpty()) {
                    logger.lifecycle("Removing javadoc for synthetic method {}.{}{}", clsData.getName(),
                            methodData.getName(), methodData.getDescriptor());
                } else {
                    newMethodData.addJavadoc(methodData.getJavadoc());
                }

                // Method Parameters
                for (MappingDataContainer.ParameterData paramData : methodData.getParameters()) {
                    final MutableParameterData newParamData = newMethodData.getOrCreateParameter(paramData.getIndex());

                    if (isSynthetic && (paramData.getName() != null || paramData.getJavadoc() != null)) {
                        logger.lifecycle("Dropping data for param #{} of synthetic method {}.{}{}", paramData.getIndex(),
                                clsData.getName(), methodData.getName(), methodData.getDescriptor());
                    } else {
                        newParamData.setName(paramData.getName()).setJavadoc(paramData.getJavadoc());
                    }
                }
            }
        }

        getInputFormat().get().write(builder, input);
    }
}
