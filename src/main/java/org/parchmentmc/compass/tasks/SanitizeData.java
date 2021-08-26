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
import org.parchmentmc.compass.util.DescriptorIndexer;
import org.parchmentmc.compass.util.MappingUtil;
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
import java.util.BitSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.parchmentmc.feather.mapping.MappingDataBuilder.*;

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

        final MappingDataContainer data = getInputFormat().get().read(input);

        final Set<String> classesToRemove = new HashSet<>();
        final Set<MutableFieldData> fieldsToRemove = new HashSet<>();
        final Set<MutableMethodData> methodsToRemove = new HashSet<>();
        final Set<Byte> paramsToRemove = new HashSet<>();

        final MappingDataBuilder builder = MappingDataBuilder.copyOf(data);

        final Map<String, ClassMetadata> classMetadataMap = MappingUtil.buildClassMetadataMap(metadata);
        final DescriptorIndexer indexer = new DescriptorIndexer();

        for (final MutableClassData classData : builder.getClasses()) {
            final ClassMetadata classMeta = classMetadataMap.get(classData.getName());
            boolean hasRemoved = false;

            for (final MutableFieldData fieldData : classData.getFields()) {
                final FieldMetadata fieldMeta = classMeta != null ? classMeta.getFields().stream()
                        .filter(s -> s.getName().getMojangName().orElse("").contentEquals(fieldData.getName()))
                        .findFirst().orElse(null) : null;

                // Remove javadocs from synthetic fields
                if (fieldMeta != null && fieldMeta.hasAccessFlag(AccessFlag.SYNTHETIC) && !fieldData.getJavadoc().isEmpty()) {
                    logger.lifecycle("Dropping synthetic field {}#{}", classData.getName(),
                            fieldData.getName());
                    fieldsToRemove.add(fieldData);
                    hasRemoved = true;
                }
            }
            fieldsToRemove.forEach(field -> classData.removeField(field.getName()));
            fieldsToRemove.clear();

            for (final MutableMethodData methodData : classData.getMethods()) {
                final MethodMetadata methodMeta = classMeta != null ? classMeta.getMethods().stream()
                        .filter(s -> s.getName().getMojangName().orElse("").contentEquals(methodData.getName())
                                && s.getDescriptor().getMojangName().orElse("").contentEquals(methodData.getDescriptor()))
                        .findFirst().orElse(null) : null;

                // Only target non-lambda synthetic methods
                if (methodMeta != null && methodMeta.hasAccessFlag(AccessFlag.SYNTHETIC) && !methodMeta.isLambda()) {
                    logger.lifecycle("Dropping synthetic method {}#{}{}", classData.getName(),
                            methodData.getName(), methodData.getDescriptor());
                    methodsToRemove.add(methodData);
                    hasRemoved = true;
                } else if (classMeta != null && classMeta.hasAccessFlag(AccessFlag.ENUM)
                        && methodData.getName().equals("valueOf")
                        && methodData.getDescriptor().equals("(Ljava/lang/String;)L" + classData.getName() + ';')) {
                    logger.lifecycle("Dropping enum `valueOf` method for {}", classData.getName(),
                            methodData.getName(), methodData.getDescriptor());
                    methodsToRemove.add(methodData);
                    hasRemoved = true;
                } else {
                    final BitSet indexes = indexer.getIndexes(methodData, methodMeta);

                    for (MutableParameterData paramData : methodData.getParameters()) {
                        byte index = paramData.getIndex();

                        if (!indexes.get(index)) {
                            paramsToRemove.add(index);
                        }
                    }

                    if (!paramsToRemove.isEmpty()) {
                        logger.lifecycle("Removing parameters {} from method {}{}#{}", paramsToRemove,
                                classData.getName(), methodData.getName(), methodData.getDescriptor());

                        paramsToRemove.forEach(methodData::removeParameter);
                        paramsToRemove.clear();

                        if (methodData.getJavadoc().isEmpty() && methodData.getParameters().isEmpty()) {
                            logger.lifecycle("Dropping empty method {}{}#{}", classData.getName(),
                                    methodData.getName(), methodData.getDescriptor());

                            methodsToRemove.add(methodData);
                            hasRemoved = true;
                        }
                    }
                }
            }
            methodsToRemove.forEach(method -> classData.removeMethod(method.getName(), method.getDescriptor()));
            methodsToRemove.clear();

            if (hasRemoved && classData.getJavadoc().isEmpty()
                    && classData.getFields().isEmpty() && classData.getMethods().isEmpty()) {
                logger.lifecycle("Empty class, dropping {}", classData.getName());
                classesToRemove.add(classData.getName());
            }
        }
        classesToRemove.forEach(builder::removeClass);

        getInputFormat().get().write(builder, input);
    }
}
