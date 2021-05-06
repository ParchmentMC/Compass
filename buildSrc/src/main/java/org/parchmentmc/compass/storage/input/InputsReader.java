package org.parchmentmc.compass.storage.input;

import net.minecraftforge.srgutils.IMappingFile;
import org.gradle.api.NamedDomainObjectCollection;
import org.gradle.internal.Pair;
import org.parchmentmc.compass.providers.IntermediateProvider;
import org.parchmentmc.compass.storage.ImmutableMappingDataContainer;
import org.parchmentmc.compass.storage.MappingDataBuilder;
import org.parchmentmc.compass.storage.MappingDataContainer;

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
            IMappingFile toObf = mapping.reverse();

            for (Path path : files) {
                insertEntries(path, toObf, builder);
            }
        }

        return builder;
    }

    // Reused data builder, to avoid excessive allocations
    private final MappingDataBuilder temp = new MappingDataBuilder();

    // Mapping should be [? -> obf] 
    private void insertEntries(Path file, IMappingFile mapping, MappingDataBuilder builder) throws IOException {
        temp.clearPackages().clearClasses();

        SimpleInputFileReader.parseLines(temp, Files.readAllLines(file));

        for (MappingDataBuilder.MutablePackageData pkg : temp.getPackages()) {
            IMappingFile.IPackage mappedPkg = mapping.getPackage(pkg.getName());
            if (mappedPkg == null) continue;

            builder.getOrCreatePackage(mappedPkg.getMapped())
                    .addJavadoc(pkg.getJavadoc());
        }

        // Copy classes
        for (MappingDataBuilder.MutableClassData cls : temp.getClasses()) {
            System.out.println(cls.getName());
            IMappingFile.IClass mappedClass = mapping.getClass(cls.getName());
            if (mappedClass == null) continue;
            System.out.println("class found: " + mappedClass.getMapped());

            MappingDataBuilder.MutableClassData classBuilder = builder.getOrCreateClass(mappedClass.getMapped())
                    .addJavadoc(cls.getJavadoc());

            // Copy fields of classes
            for (MappingDataBuilder.MutableFieldData field : cls.getFields()) {
                IMappingFile.IField mappedField = mappedClass.getField(field.getName());
                if (mappedField == null) continue;

                classBuilder.getOrCreateField(mappedField.getMapped())
                        .setDescriptor(mapping.remapDescriptor(field.getDescriptor()))
                        .addJavadoc(field.getJavadoc());
            }

            // Copy methods of classes
            for (MappingDataBuilder.MutableMethodData method : cls.getMethods()) {
                System.out.println("m: " + method.getName() + " " + method.getDescriptor());
                IMappingFile.IMethod mappedMethod = mappedClass.getMethod(method.getName(), method.getDescriptor());
                if (mappedMethod == null) continue;
                System.out.println("method found: " + mappedMethod.getMapped() + " " + mappedMethod.getMappedDescriptor());

                MappingDataBuilder.MutableMethodData methodBuilder = classBuilder.getOrCreateMethod(
                        mappedMethod.getMapped(), mappedMethod.getMappedDescriptor())
                        .addJavadoc(method.getJavadoc());

                Map<Integer, ? extends IMappingFile.IParameter> params = mappedMethod.getParameters().stream()
                        .collect(Collectors.toMap(IMappingFile.IParameter::getIndex, t -> t)); // TODO: cache this?
                for (MappingDataContainer.ParameterData param : method.getParameters()) {
                    IMappingFile.IParameter mappedParam = params.get((int) param.getIndex());
                    if (mappedParam != null) continue;

                    methodBuilder.getOrCreateParameter(param.getIndex())
                            .setName(param.getName())
                            .setJavadoc(param.getJavadoc());
                }
            }
        }

    }
}
