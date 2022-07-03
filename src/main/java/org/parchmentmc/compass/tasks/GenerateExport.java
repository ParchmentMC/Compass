package org.parchmentmc.compass.tasks;

import com.google.common.base.Strings;
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
import org.parchmentmc.feather.metadata.BouncingTargetMetadata;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;
import org.parchmentmc.feather.metadata.RecordMetadata;
import org.parchmentmc.feather.metadata.Reference;
import org.parchmentmc.feather.metadata.SourceMetadata;
import org.parchmentmc.feather.metadata.WithName;
import org.parchmentmc.feather.metadata.WithType;
import org.parchmentmc.feather.named.Named;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class GenerateExport extends DefaultTask {
    private static final String CONSTRUCTOR_METHOD_NAME = "<init>";
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

        builder.getClasses().forEach(clsData -> copyRecordData(clsData, classMetadataMap.get(clsData.getName())));

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
        if (getUseBlackstone().get()) {
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
            // Create method data at the bouncer target if it exists, otherwise default to the current method.
            Supplier<MappingDataBuilder.MutableMethodData> supplier = methodMeta.getBouncingTarget()
                    .flatMap(BouncingTargetMetadata::getTarget)
                    .<Supplier<MappingDataBuilder.MutableMethodData>>map(ref -> () ->
                            builder.getOrCreateClass(getMojangName(ref.getOwner()))
                                    .getOrCreateMethod(getMojangName(ref.getName()), getMojangName(ref.getDescriptor())))
                    .orElse(() -> clsData.getOrCreateMethod(name, desc));
            GenerateExport.cascadeParentMethod(builder, classMetadataMap, methodMeta, supplier);
        });
    }

    /**
     * This code cascades parameters and javadocs from parent methods,
     * stopping at the first one that has something populated.
     */
    private static void cascadeParentMethod(MappingDataBuilder builder, Map<String, ClassMetadata> classMetadataMap, MethodMetadata methodMeta,
            Supplier<MappingDataBuilder.MutableMethodData> methodDataSupplier) {
        MappingDataContainer.MethodData parentMethodData = findParentMethodData(builder, classMetadataMap, methodMeta);

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

    @Nullable
    private static MappingDataContainer.MethodData findParentMethodData(MappingDataContainer builder, Map<String, ClassMetadata> classMetadataMap, MethodMetadata startingMethodMeta) {
        // Non-null only if the method is a constructor
        // Since we only check for the same descriptor when looking at constructors, we store this once and reuse
        String constructorDescriptor = CONSTRUCTOR_METHOD_NAME.equals(getMojangName(startingMethodMeta.getName()))
                ? getMojangName(startingMethodMeta.getDescriptor())
                : null;
        
        MethodMetadata currentMethodMeta = startingMethodMeta;
        MappingDataContainer.MethodData currentMethodData = null;
        
        // Continue while we haven't found (populated) method data yet and we still have method meta to check
        while (currentMethodData == null && currentMethodMeta != null) {
            String parentOwner, parentName, parentDescriptor;
            if (constructorDescriptor != null) {
                // Special-case for constructors, which do not have override info in their metadata
                // We match for a method with the same descriptor in their direct superclass instead

                // Get the name of the superclass
                final ClassMetadata parentOwnerMeta = Objects.requireNonNull(classMetadataMap.get(getMojangName(currentMethodMeta.getOwner())));
                parentOwner = getMojangName(parentOwnerMeta.getSuperName());
                parentName = CONSTRUCTOR_METHOD_NAME;
                parentDescriptor = constructorDescriptor;

                // We don't check here if the superclass constructor with same descriptor exists here,
                // as that'll duplicate the checking done by the loop condition
            } else {
                if (!currentMethodMeta.getParent().isPresent()) {
                    break; // Break out if there is no parent to check
                }
                Reference parent = currentMethodMeta.getParent().get();

                parentOwner = getMojangName(parent.getOwner());
                parentName = getMojangName(parent.getName());
                parentDescriptor = getMojangName(parent.getDescriptor());
            }

            // Query the actual mapping data to see if the parent method has any javadocs or parameters
            currentMethodData = Optional.ofNullable(builder.getClass(parentOwner))
                    .map(c -> c.getMethod(parentName, parentDescriptor))
                    .filter(m -> !m.getJavadoc().isEmpty() || m.getParameters().stream().anyMatch(p -> p.getJavadoc() != null || p.getName() != null))
                    .orElse(null);

            // Get the new method metadata so we can get the next parent
            currentMethodMeta = MappingUtil.getMethodMetadata(classMetadataMap.get(parentOwner),
                    parentName, parentDescriptor);
        }

        return currentMethodData;
    }
    
    protected static void copyRecordData(MappingDataBuilder.MutableClassData classData, @Nullable ClassMetadata classMeta) {
        if (classMeta == null || !classMeta.isRecord()) return;
        
        // As per JLS, record class fields correspond 1-to-1 with record components, in the same order
        final List<String> recordNames = classMeta.getRecords().stream()
                .map(RecordMetadata::getField)
                .map(WithName::getName)
                .map(named -> named.getMojangName().orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        final Map<String, MappingDataBuilder.MutableFieldData> recordFields = recordNames.stream()
                .map(classData::getField)
                .filter(Objects::nonNull)
                .filter(s -> !s.getJavadoc().isEmpty()) // Only record fields with javadocs
                .collect(Collectors.toMap(MappingDataBuilder.MutableFieldData::getName, Function.identity()));
        
        if (recordFields.isEmpty()) return; // No fields with javadocs, so exit out early

        final MappingDataBuilder.MutableMethodData canonicalConstructor = 
                classData.getOrCreateMethod(CONSTRUCTOR_METHOD_NAME, createCanonicalConstructorDescriptor(classMeta));

        for (int i = 0; i < recordNames.size(); i++) {
            final String recordComponentName = recordNames.get(i);
            final MappingDataBuilder.MutableFieldData recordField = recordFields.get(recordComponentName);

            // Always define canonical constructor params
            final MappingDataBuilder.MutableParameterData paramData = canonicalConstructor.createParameter((byte) (i + 1));
            paramData.setName(recordComponentName);
            
            if (recordField == null) continue; // No field, no javadocs

            // Canonical constructor javadoc
            if (paramData.getJavadoc() == null) {
                paramData.addJavadoc(recordField.getJavadoc());
            }

            // Class javadoc
            final List<String> javadocs = new ArrayList<>(recordField.getJavadoc());
            final String header = "@param " + recordComponentName + " ";
            classData.addJavadoc("@param " + recordComponentName + " " + javadocs.remove(0));

            final String spacePrefix = Strings.repeat(" ", header.length()); // Prefix remaining lines with spaces
            for (String javadocLine : javadocs) {
                if (!javadocLine.isEmpty()) {
                    classData.addJavadoc(spacePrefix + javadocLine);
                }
            }
        }
    }

    private static String createCanonicalConstructorDescriptor(ClassMetadata classMeta) {
        final String params = classMeta.getRecords().stream()
                .map(RecordMetadata::getField)
                .map(WithType::getDescriptor)
                .map(named -> named.getMojangName().orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.joining(""));

        return "(" + params + ")V";
    }

    private static String getMojangName(Named named) {
        return named.getMojangName().orElse("");
    }
}
