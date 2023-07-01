package org.parchmentmc.compass.util;

import net.minecraftforge.srgutils.IMappingBuilder;
import net.minecraftforge.srgutils.IMappingFile;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.util.download.ObfuscationMapsDownloader;
import org.parchmentmc.feather.mapping.MappingDataBuilder;
import org.parchmentmc.feather.mapping.MappingDataContainer;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.FieldMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;
import org.parchmentmc.feather.metadata.SourceMetadata;
import org.parchmentmc.feather.named.Named;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.parchmentmc.feather.mapping.MappingDataBuilder.MutableClassData;
import static org.parchmentmc.feather.mapping.MappingDataBuilder.MutableFieldData;
import static org.parchmentmc.feather.mapping.MappingDataBuilder.MutableMethodData;
import static org.parchmentmc.feather.mapping.MappingDataBuilder.MutablePackageData;
import static org.parchmentmc.feather.mapping.MappingDataBuilder.MutableParameterData;
import static org.parchmentmc.feather.mapping.MappingDataBuilder.copyOf;

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
        mappingFile.getPackages().forEach(pkg -> builder.createPackage(reversed ? pkg.getMapped() : pkg.getOriginal()));

        // Copy classes
        mappingFile.getClasses().forEach(cls -> {
            MutableClassData classBuilder = builder.createClass(reversed ? cls.getMapped() : cls.getOriginal());

            // Copy fields of classes
            cls.getFields().forEach(field ->
                    classBuilder.createField(reversed ? field.getMapped() : field.getOriginal(),
                            reversed ? field.getMappedDescriptor() : field.getDescriptor()));

            // Copy methods of classes
            cls.getMethods().forEach(method -> {
                MutableMethodData methodBuilder = classBuilder.createMethod(
                        reversed ? method.getMapped() : method.getOriginal(),
                        reversed ? method.getMappedDescriptor() : method.getDescriptor());

                // Copy parameters of methods
                method.getParameters().forEach(param ->
                        methodBuilder.createParameter((byte) param.getIndex())
                                .setName(reversed ? param.getMapped() : param.getOriginal()));
            });
        });

        return builder;
    }

    public static MappingDataBuilder constructPackageData(MappingDataContainer container) {
        if (container instanceof MappingDataBuilder) {
            return constructPackageData((MappingDataBuilder) container);
        }
        return constructPackageData(copyOf(container));
    }

    // Creates packages based on the classes
    public static MappingDataBuilder constructPackageData(MappingDataBuilder builder) {
        for (MutableClassData cls : builder.getClasses()) {
            int lastIndex = cls.getName().lastIndexOf('/');
            if (lastIndex != -1) { // has a package
                String pkgName = cls.getName().substring(0, lastIndex);
                builder.getOrCreatePackage(pkgName);
            }
        }

        return builder;
    }

    /**
     * Constructs packages for the given mapping file based on the class names, and returns a new mapping file with the
     * constructed packages.
     *
     * <p>Packages constructed by this method (whether pre-existing or not) will have the {@code constructed} metadata key with
     * a value of {@code "true"}.</p>
     *
     * @param mapping The mapping file to construct pages
     * @return a copy of the mapping file with the constructed packages
     */
    public static IMappingFile constructPackageData(IMappingFile mapping) {
        IMappingBuilder builder = IMappingBuilder.create("left", "right");

        for (IMappingFile.IPackage pkg : mapping.getPackages()) {
            IMappingBuilder.IPackage newPkg = builder.addPackage(pkg.getOriginal(), pkg.getMapped());
            pkg.getMetadata().forEach(newPkg::meta);
        }

        for (IMappingFile.IClass cls : mapping.getClasses()) {
            int lastIndex = cls.getOriginal().lastIndexOf('/');
            if (lastIndex != -1) {
                String pkgName = cls.getOriginal().substring(0, lastIndex);
                builder.addPackage(pkgName, pkgName)
                        .meta("constructed", "true");
            }

            IMappingBuilder.IClass newCls = builder.addClass(cls.getOriginal(), cls.getMapped());
            cls.getMetadata().forEach(newCls::meta);

            for (IMappingFile.IField field : cls.getFields()) {
                IMappingBuilder.IField newField = newCls.field(field.getOriginal(), field.getMapped())
                        .descriptor(field.getDescriptor());
                field.getMetadata().forEach(newField::meta);
            }

            for (IMappingFile.IMethod method : cls.getMethods()) {
                IMappingBuilder.IMethod newMethod = newCls.method(method.getDescriptor(), method.getOriginal(), method.getMapped());
                method.getMetadata().forEach(newMethod::meta);

                for (IMappingFile.IParameter param : method.getParameters()) {
                    IMappingBuilder.IParameter newParam = newMethod.parameter(param.getIndex(), param.getOriginal(), param.getMapped());
                    param.getMetadata().forEach(newParam::meta);
                }
            }
        }

        return builder.build().getMap("left", "right");
    }

    // Mapping should be names from data -> target names
    public static MappingDataContainer remapData(MappingDataContainer data, IMappingFile mapping) {
        MappingDataBuilder builder = new MappingDataBuilder();

        data.getPackages().forEach(pkg -> builder.createPackage(mapping.remapPackage(pkg.getName())).addJavadoc(pkg.getJavadoc()));

        data.getClasses().forEach(cls -> {
            IMappingFile.IClass mappedClass = mapping.getClass(cls.getName());
            String clsName = cls.getName();
            if (mappedClass != null) {
                clsName = mappedClass.getMapped();
            }
            MutableClassData classData = builder.createClass(clsName).addJavadoc(cls.getJavadoc());

            cls.getFields().forEach(field -> {
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

                classData.createField(fieldName, fieldDescriptor).addJavadoc(field.getJavadoc());
            });

            cls.getMethods().forEach(method -> {
                String methodName = method.getName();
                String methodDescriptor = method.getDescriptor();

                if (mappedClass != null) {
                    IMappingFile.IMethod mappedMethod = mappedClass.getMethod(method.getName(), method.getDescriptor());
                    if (mappedMethod != null) {
                        methodName = mappedMethod.getMapped();
                        methodDescriptor = mappedMethod.getMappedDescriptor();
                    }
                }

                MutableMethodData methodData = classData.createMethod(methodName, methodDescriptor)
                        .addJavadoc(method.getJavadoc());

                // TODO: determine better logic for handling parameters
                method.getParameters().forEach(param -> methodData.createParameter(param.getIndex()).setName(param.getName()).setJavadoc(param.getJavadoc()));
            });
        });

        return builder;
    }

    public static void copyData(MappingDataContainer base, MappingDataBuilder builder) {
        // Copy packages
        base.getPackages().forEach(pkg -> builder.createPackage(pkg.getName()).addJavadoc(pkg.getJavadoc()));

        // Copy classes
        base.getClasses().forEach(cls -> {
            MutableClassData classData = builder.createClass(cls.getName()).addJavadoc(cls.getJavadoc());

            // Copy fields
            cls.getFields().forEach(field -> classData.createField(field.getName(), field.getDescriptor()).addJavadoc(field.getJavadoc()));

            // Copy methods
            cls.getMethods().forEach(method -> {
                MutableMethodData methodData = classData.createMethod(method.getName(), method.getDescriptor()).addJavadoc(method.getJavadoc());

                // Copy parameters
                method.getParameters().forEach(param -> methodData.createParameter(param.getIndex()).setName(param.getName()).setJavadoc(param.getJavadoc()));
            });
        });
    }

    public static MappingDataBuilder loadOfficialData(ObfuscationMapsDownloader obfuscationMapsDownloader) {
        IMappingFile obfMap = obfuscationMapsDownloader.getObfuscationMap().get();
        return loadOfficialData(obfMap);
    }

    public static MappingDataBuilder loadOfficialData(IMappingFile obfToMoj) {
        return constructPackageData(createBuilderFrom(obfToMoj, false));
    }

    public static void removeUndocumented(MappingDataBuilder builder) {
        builder.getPackages().stream()
                .filter(s -> s.getJavadoc().isEmpty())
                .map(MutablePackageData::getName)
                .collect(Collectors.toSet())
                .forEach(builder::removePackage);

        //noinspection Convert2MethodRef damn you javac
        builder.getClasses().stream()
                .peek(cls -> cls.getFields().stream()
                        .filter(field -> field.getJavadoc().isEmpty())
                        .map(MutableFieldData::getName)
                        .collect(Collectors.toSet())
                        .forEach(field -> cls.removeField(field)))
                .peek(cls -> cls.getMethods().stream()
                        .peek(method -> method.getParameters().stream()
                                .filter(param -> param.getName() == null && param.getJavadoc() == null)
                                .map(MutableParameterData::getIndex)
                                .collect(Collectors.toSet())
                                .forEach(method::removeParameter))
                        .filter(method -> method.getJavadoc().isEmpty() && method.getParameters().isEmpty())
                        .collect(Collectors.toSet())
                        .forEach(method -> cls.removeMethod(method.getName(), method.getDescriptor())))
                .filter(cls -> cls.getJavadoc().isEmpty() && cls.getFields().isEmpty() && cls.getMethods().isEmpty())
                .map(MutableClassData::getName)
                .collect(Collectors.toSet())
                .forEach(builder::removeClass);
    }

    public static Map<String, ClassMetadata> buildClassMetadataMap(@Nullable SourceMetadata metadata) {
        if (metadata == null) return Collections.emptyMap();

        final HashMap<String, ClassMetadata> classMetadataMap = new HashMap<>();
        final ArrayDeque<ClassMetadata> toTraverse = new ArrayDeque<>(metadata.getClasses());

        ClassMetadata current;
        while ((current = toTraverse.poll()) != null) {
            classMetadataMap.put(current.getName().getMojangName().orElse(""), current);
            toTraverse.addAll(current.getInnerClasses());
        }

        return classMetadataMap;
    }

    /**
     * Returns the matching field metadata from the class metadata according to the name from the naming function.
     *
     * @param nameFunction  the naming function
     * @param classMetadata the class metadata, may be {@code null}
     * @param fieldName     the field name
     * @return the matching field metadata, or {@code null} if either the class metadata is {@code null} or there is no
     * field metadata in the class which has a matching name
     */
    @Nullable
    private static FieldMetadata getFieldMetadata(Function<Named, Optional<String>> nameFunction,
                                                  @Nullable ClassMetadata classMetadata, String fieldName) {
        if (classMetadata == null) return null;

        return classMetadata.getFields().stream()
                .filter(s -> nameFunction.apply(s.getName()).orElse("").contentEquals(fieldName))
                .findFirst().orElse(null);
    }

    /**
     * Returns the matching field metadata from the class metadata according to {@linkplain Named#getMojangName()
     * Mojang names}.
     *
     * @param classMetadata the class metadata, may be {@code null}
     * @param fieldName     the field name
     * @return the matching field metadata, or {@code null} if either the class metadata is {@code null} or there is no
     * field metadata in the class which has a matching Mojang name
     * @see #getFieldMetadata(Function, ClassMetadata, String)
     */
    @Nullable
    public static FieldMetadata getFieldMetadata(@Nullable ClassMetadata classMetadata, String fieldName) {
        return getFieldMetadata(Named::getMojangName, classMetadata, fieldName);
    }

    /**
     * Returns the matching method metadata from the class metadata according to the names from the naming function.
     *
     * @param nameFunction  the naming function
     * @param classMetadata the class metadata, may be {@code null}
     * @param methodName    the method name
     * @param methodDesc    the method description
     * @return the matching method metadata, or {@code null} if either the class metadata is {@code null} or there is no
     * method metadata in the class which has a matching name and descriptor
     */
    @Nullable
    private static MethodMetadata getMethodMetadata(Function<Named, Optional<String>> nameFunction,
                                                    @Nullable ClassMetadata classMetadata, String methodName, String methodDesc) {
        if (classMetadata == null) return null;

        return classMetadata.getMethods().stream()
                .filter(s -> nameFunction.apply(s.getName()).orElse("").contentEquals(methodName)
                        && nameFunction.apply(s.getDescriptor()).orElse("").contentEquals(methodDesc))
                .findFirst().orElse(null);
    }

    /**
     * Returns the matching method metadata from the class metadata according to {@linkplain Named#getMojangName()
     * Mojang names}.
     *
     * @param classMetadata the class metadata, may be {@code null}
     * @param methodName    the method name
     * @param methodDesc    the method description
     * @return the matching method metadata, or {@code null} if either the class metadata is {@code null} or there is no
     * method metadata in the class which has a matching name and descriptor with Mojang names
     * @see #getMethodMetadata(Function, ClassMetadata, String, String)
     */
    @Nullable
    public static MethodMetadata getMethodMetadata(@Nullable ClassMetadata classMetadata, String methodName, String methodDesc) {
        return getMethodMetadata(Named::getMojangName, classMetadata, methodName, methodDesc);
    }
}
