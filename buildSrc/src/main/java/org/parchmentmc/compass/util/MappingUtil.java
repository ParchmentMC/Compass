package org.parchmentmc.compass.util;

import net.minecraftforge.srgutils.IMappingFile;
import org.parchmentmc.compass.storage.MappingDataBuilder;
import org.parchmentmc.compass.storage.MappingDataContainer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class MappingUtil {

    public static IMappingFile loadAndEnsureSuperset(Path client, Path server) {
        IMappingFile clientMap, serverMap;
        try (InputStream clientInput = Files.newInputStream(client);
             InputStream serverInput = Files.newInputStream(server)) {
            clientMap = IMappingFile.load(clientInput);
            serverMap = IMappingFile.load(serverInput);
        } catch (IOException e) {
            throw new RuntimeException("Exception while loading client and server obfuscation maps", e);
        }

        boolean superset = isSuperset(serverMap, clientMap);
        if (!superset) {
            throw new RuntimeException("'wot'; Client obfuscation map is not a superset of server obfuscation map");
        }
        return clientMap;
    }

    /**
     * Returns {@code true} if the {@code superset} mapping file is a superset of the {@code set} mapping file.
     *
     * @param set      The target mapping file
     * @param superset The superset mapping file
     * @return {@code true} if {@code superset} is a superset of {@code set}
     */
    static boolean isSuperset(IMappingFile set, IMappingFile superset) {
        List<String> superSetPackages = superset.getPackages().stream().map(Object::toString).collect(Collectors.toList());
        // Check if all packages are present in superset
        if (!set.getPackages().stream().map(Object::toString).allMatch(superSetPackages::contains)) return false;

        List<String> superSetClasses = superset.getClasses().stream().map(IMappingFile.INode::getOriginal).collect(Collectors.toList());
        for (IMappingFile.IClass cls : set.getClasses()) {
            // Check that class is present in superset
            if (!superSetClasses.contains(cls.getOriginal())) return false;
            IMappingFile.IClass superCls = superset.getClass(cls.getOriginal());

            List<String> superClsFields = superCls.getFields().stream().map(Object::toString).collect(Collectors.toList());
            // Check if all fields are present in superclass
            if (!cls.getFields().stream().map(Object::toString).allMatch(superClsFields::contains)) return false;

            for (IMappingFile.IMethod method : cls.getMethods()) {
                // Check that method is present in superclass
                IMappingFile.IMethod superMethod = superCls.getMethod(method.getOriginal(), method.getDescriptor());
                if (superMethod == null) return false;
            }
        }

        return true;
    }

    public static MappingDataBuilder createBuilderFrom(IMappingFile mappingFile, boolean reversed) {
        MappingDataBuilder builder = new MappingDataBuilder();

        // Copy packages
        mappingFile.getPackages().forEach(pkg -> builder.addPackage(reversed ? pkg.getMapped() : pkg.getOriginal()));

        // Copy classes
        mappingFile.getClasses().forEach(cls -> {
            MappingDataBuilder.MutableClassData classBuilder = builder.addClass(reversed ? cls.getMapped() : cls.getOriginal());

            // Copy fields of classes
            cls.getFields().forEach(field ->
                    classBuilder.addField(reversed ? field.getMapped() : field.getOriginal())
                            .setDescriptor(reversed ? field.getMappedDescriptor() : field.getDescriptor()));

            // Copy methods of classes
            cls.getMethods().forEach(method -> {
                MappingDataBuilder.MutableMethodData methodBuilder = classBuilder.addMethod(
                        reversed ? method.getMapped() : method.getOriginal(),
                        reversed ? method.getMappedDescriptor() : method.getDescriptor());

                // Copy parameters of methods
                method.getParameters().forEach(param ->
                        methodBuilder.addParameter((byte) param.getIndex())
                                .setName(reversed ? param.getMapped() : param.getOriginal()));
            });
        });

        return builder;
    }

    public static MappingDataBuilder createBuilderFrom(MappingDataContainer container) {
        MappingDataBuilder builder = new MappingDataBuilder();

        // Copy packages
        container.getPackages().forEach(pkg -> builder.addPackage(pkg.getName()).addJavadoc(pkg.getJavadoc()));

        // Copy classes
        container.getClasses().forEach(cls -> {
            MappingDataBuilder.MutableClassData classData = builder.addClass(cls.getName()).addJavadoc(cls.getJavadoc());

            // Copy fields
            cls.getFields().forEach(field -> classData.addField(field.getName()).setDescriptor(field.getDescriptor()).addJavadoc(field.getJavadoc()));

            // Copy methods
            cls.getMethods().forEach(method -> {
                MappingDataBuilder.MutableMethodData methodData = classData.addMethod(method.getName(), method.getDescriptor()).addJavadoc(method.getJavadoc());

                // Copy parameters
                method.getParameters().forEach(param -> methodData.addParameter(param.getIndex()).setName(param.getName()).setJavadoc(param.getJavadoc()));
            });
        });

        return builder;
    }
}
