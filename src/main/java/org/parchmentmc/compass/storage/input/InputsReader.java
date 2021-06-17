package org.parchmentmc.compass.storage.input;

import net.minecraftforge.srgutils.IMappingFile;
import org.gradle.api.NamedDomainObjectCollection;
import org.gradle.internal.Pair;
import org.parchmentmc.compass.providers.IntermediateProvider;
import org.parchmentmc.compass.storage.io.SingleFileDataIO;
import org.parchmentmc.compass.util.MappingUtil;
import org.parchmentmc.feather.mapping.ImmutableMappingDataContainer;
import org.parchmentmc.feather.mapping.MappingDataBuilder;
import org.parchmentmc.feather.mapping.MappingDataContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/*
Subdirectories within the input folder denote what mapping type is within
 */
public class InputsReader {
    private final NamedDomainObjectCollection<IntermediateProvider> intermediates;

    public InputsReader(NamedDomainObjectCollection<IntermediateProvider> intermediates) {
        this.intermediates = intermediates;
    }

    public MappingDataContainer parse(Path base) throws IOException {
        List<Path> subdirs = Files.list(base)
                .filter(Files::isDirectory)
                .collect(Collectors.toList());
        // Skip if there are no directories
        if (subdirs.isEmpty()) {
            return new ImmutableMappingDataContainer(Collections.emptyList(), Collections.emptyList());
        }

        MappingDataBuilder builder = new MappingDataBuilder();

        List<Pair<IntermediateProvider, Path>> directories = new ArrayList<>();

        final IntermediateProvider officialProvider = intermediates.getByName("official");
        final IMappingFile obfToOfficial = officialProvider.getMapping();

        for (Path subdir : subdirs) {
            String dirName = subdir.getFileName().toString();

            IntermediateProvider provider = intermediates.findByName(dirName);
            // Ignore directories without a corresponding intermediate
            if (provider != null) {
                System.out.println("Found intermediate for " + dirName);
                directories.add(Pair.of(provider, subdir));
            }
        }

        // Skip if there are no eligible directories for reading
        if (directories.isEmpty()) {
            return new ImmutableMappingDataContainer(Collections.emptyList(), Collections.emptyList());
        }

        for (Pair<IntermediateProvider, Path> dir : directories) {
            Set<Path> files = Files.list(Objects.requireNonNull(dir.getRight())).filter(Files::isRegularFile).collect(Collectors.toSet());
            // Skip if there are no files within this directory
            if (files.isEmpty()) {
                continue;
            }

            IntermediateProvider provider = Objects.requireNonNull(dir.getLeft());
            IMappingFile mapping = provider.getMapping();
            IMappingFile toOfficial = mapping.reverse().chain(obfToOfficial);

            for (Path path : files) {
                insertEntries(path, toOfficial, builder);
            }
        }

        return builder;
    }

    // Reused data builder, to avoid excessive allocations
    private final MappingDataBuilder temp = new MappingDataBuilder();

    // Mapping should be [? -> official]
    private void insertEntries(Path file, IMappingFile mapping, MappingDataBuilder builder) throws IOException {
        temp.clearPackages().clearClasses();

        String filename = file.getFileName().toString();
        if (filename.endsWith(".txt")) {
            SimpleInputFileReader.parseLines(temp, Files.readAllLines(file));
        } else if (filename.endsWith(".json")) {
            MappingUtil.copyData(SingleFileDataIO.INSTANCE.read(file), temp);
        } else {
            return;
        }

        for (MappingDataBuilder.MutablePackageData pkg : temp.getPackages()) {
            builder.getOrCreatePackage(mapping.remapPackage(pkg.getName()))
                    .addJavadoc(pkg.getJavadoc());
        }

        // Copy classes
        for (MappingDataBuilder.MutableClassData cls : temp.getClasses()) {
            String clsName = cls.getName();

            IMappingFile.IClass mappedClass = mapping.getClass(cls.getName());
            if (mappedClass != null) {
                clsName = mappedClass.getMapped();
            }

            MappingDataBuilder.MutableClassData classBuilder = builder.getOrCreateClass(clsName)
                    .addJavadoc(cls.getJavadoc());

            // Copy fields of classes
            for (MappingDataBuilder.MutableFieldData field : cls.getFields()) {
                String fieldName = field.getName();
                String fieldDescriptor = mapping.remapDescriptor(field.getDescriptor());

                if (mappedClass != null) {
                    IMappingFile.IField mappedField = mappedClass.getField(field.getName());
                    if (mappedField != null) {
                        fieldName = mappedField.getMapped();
                        if (mappedField.getMappedDescriptor() != null) {
                            fieldDescriptor = mappedField.getMappedDescriptor();
                        }
                    }
                }

                classBuilder.getOrCreateField(fieldName, fieldDescriptor).addJavadoc(field.getJavadoc());
            }

            // Copy methods of classes
            for (MappingDataBuilder.MutableMethodData method : cls.getMethods()) {
                String methodName = method.getName();
                String methodDescriptor = method.getDescriptor();

                if (mappedClass != null) {
                    IMappingFile.IMethod mappedMethod = mappedClass.getMethod(method.getName(), method.getDescriptor());
                    if (mappedMethod != null) {
                        methodName = mappedMethod.getMapped();
                        methodDescriptor = mappedMethod.getMappedDescriptor();
                    }
                }

                MappingDataBuilder.MutableMethodData methodBuilder = classBuilder.getOrCreateMethod(methodName, methodDescriptor)
                        .addJavadoc(method.getJavadoc());

                // TODO: determine better logic for handling parameters
                method.getParameters().forEach(param -> methodBuilder.getOrCreateParameter(param.getIndex()).setName(param.getName()).setJavadoc(param.getJavadoc()));
            }
        }

    }
}
