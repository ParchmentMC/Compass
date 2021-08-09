package org.parchmentmc.compass.tasks;

import com.squareup.moshi.Moshi;
import net.minecraftforge.srgutils.IMappingFile;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.Nullable;
import org.parchmentmc.compass.CompassPlugin;
import org.parchmentmc.compass.providers.IntermediateProvider;
import org.parchmentmc.compass.storage.io.MappingIOFormat;
import org.parchmentmc.compass.storage.io.SingleFileDataIO;
import org.parchmentmc.compass.util.MappingUtil;
import org.parchmentmc.compass.util.download.BlackstoneDownloader;
import org.parchmentmc.feather.io.moshi.MDCMoshiAdapter;
import org.parchmentmc.feather.io.moshi.SimpleVersionAdapter;
import org.parchmentmc.feather.mapping.MappingDataBuilder;
import org.parchmentmc.feather.mapping.MappingDataContainer;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;
import org.parchmentmc.feather.metadata.MethodReference;
import org.parchmentmc.feather.metadata.SourceMetadata;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class GenerateExport extends DefaultTask {
    private static final SingleFileDataIO IO = new SingleFileDataIO(new Moshi.Builder()
            .add(new MDCMoshiAdapter(true))
            .add(new SimpleVersionAdapter()).build(), "  ");

    public GenerateExport() {
        getOutput().convention(getProject().getLayout().getBuildDirectory().dir(getName()).map(d -> d.file("export.json")));
        getUseBlackstone().convention(Boolean.FALSE);

        onlyIf(_t -> getInput().get().getAsFile().exists());
    }

    @TaskAction
    public void export() throws IOException {
        CompassPlugin plugin = getProject().getPlugins().getPlugin(CompassPlugin.class);
        IMappingFile officialMap = plugin.getObfuscationMapsDownloader().getObfuscationMap().get(); // moj -> obf

        IntermediateProvider intermediate = plugin.getIntermediates().getByName(getIntermediate().get());
        IMappingFile mapping = intermediate.getMapping(); // obf -> ?
        IMappingFile officialToIntermediate = officialMap.chain(mapping); // [moj -> obf] -> [obf -> ?] => moj -> ?

        MappingDataContainer data = getInputFormat().get().read(getInput().get().getAsFile());

        MappingDataContainer remappedData = MappingUtil.remapData(data, officialToIntermediate);

        MappingDataContainer output = modifyData(remappedData);

        IO.write(output, getOutput().get().getAsFile());
    }

    protected MappingDataContainer modifyData(MappingDataContainer container) throws IOException {
        final MappingDataBuilder builder = MappingDataBuilder.copyOf(container);
        final SourceMetadata metadata = getSourceMetadata();
        final Map<String, ClassMetadata> classMetadataMap = MappingUtil.buildClassMetadataMap(metadata);

        builder.getClasses().forEach(clsData -> cascadeParentMethods(builder, classMetadataMap, clsData, classMetadataMap.get(clsData.getName())));

        return builder;
    }

    @InputDirectory
    public abstract DirectoryProperty getInput();

    @Input
    public abstract Property<MappingIOFormat> getInputFormat();

    @Input
    public abstract Property<String> getIntermediate();

    @OutputFile
    public abstract RegularFileProperty getOutput();

    @Input
    public abstract Property<Boolean> getUseBlackstone();

    @Nullable
    @Internal
    protected SourceMetadata getSourceMetadata() throws IOException {
        if (getUseBlackstone().get() == Boolean.TRUE) {
            final BlackstoneDownloader blackstoneDownloader = getProject().getPlugins()
                    .getPlugin(CompassPlugin.class).getBlackstoneDownloader();
            return blackstoneDownloader.retrieveMetadata();
        } else {
            return null;
        }
    }

    protected static void cascadeParentMethods(MappingDataBuilder builder, Map<String, ClassMetadata> classMetadataMap, MappingDataBuilder.MutableClassData clsData, ClassMetadata clsMeta) {
        if (clsMeta == null)
            return;
        // We need to cascade data using the class metadata methods because methods with no mapped data will not be present in ClassData#getMethods()
        clsMeta.getMethods().forEach(methodMeta -> {
            String name = methodMeta.getName().getMojangName().orElse(null);
            String desc = methodMeta.getDescriptor().getMojangName().orElse(null);
            if (name == null || desc == null)
                return;
            GenerateExport.cascadeParentMethod(builder, classMetadataMap, methodMeta, () -> clsData.getOrCreateMethod(name, desc));
        });
    }

    /**
     * This code cascades parameters and javadocs from parent methods,
     * stopping at the first one that has something populated.
     */
    private static void cascadeParentMethod(MappingDataBuilder builder, Map<String, ClassMetadata> classMetadataMap, MethodMetadata methodMeta,
            Supplier<MappingDataBuilder.MutableMethodData> methodDataSupplier) {
        MethodMetadata parentMethodMeta = methodMeta;
        MappingDataBuilder.MutableMethodData parentMethodData = null;

        while (parentMethodData == null && parentMethodMeta != null && parentMethodMeta.getParent().isPresent()) {
            MethodReference parent = parentMethodMeta.getParent().get();
            // Get the current method metadata so we can get the next parent
            parentMethodMeta = classMetadataMap.get(parent.getOwner().getMojangName().orElse("")).getMethods().stream()
                    .filter(m -> m.getName().getMojangName().equals(parent.getName().getMojangName())
                            && m.getDescriptor().getMojangName().equals(parent.getDescriptor().getMojangName()))
                    .findFirst().orElse(null);
            // Query the actual mapping data to see if the parent method has any javadocs or parameters
            parentMethodData = Optional.ofNullable(builder.getClass(parent.getOwner().getMojangName().orElse("")))
                    .map(c -> c.getMethod(parent.getName().getMojangName().orElse(""), parent.getDescriptor().getMojangName().orElse("")))
                    .filter(m -> !m.getJavadoc().isEmpty() || m.getParameters().stream().anyMatch(p -> p.getJavadoc() != null || p.getName() != null))
                    .orElse(null);
        }

        // This code cascades the data only if there is as valid parent method with mapping data
        if (parentMethodData != null) {
            MappingDataBuilder.MutableMethodData methodData = methodDataSupplier.get();
            if (methodData.getJavadoc().isEmpty())
                methodData.addJavadoc(parentMethodData.getJavadoc());

            parentMethodData.getParameters().forEach(parentParam -> {
                byte idx = parentParam.getIndex();
                MappingDataBuilder.MutableParameterData thisParam = methodData.getParameter(idx);

                // Cascade the parameter name only if the current parameter doesn't have it
                if ((thisParam == null || thisParam.getName() == null) && parentParam.getName() != null)
                    methodData.getOrCreateParameter(idx).setName(parentParam.getName());

                // Cascade the parameter javadocs only if the current parameter doesn't have it
                if ((thisParam == null || thisParam.getJavadoc() == null) && parentParam.getJavadoc() != null)
                    methodData.getOrCreateParameter(idx).setJavadoc(parentParam.getJavadoc());
            });
        }
    }
}
