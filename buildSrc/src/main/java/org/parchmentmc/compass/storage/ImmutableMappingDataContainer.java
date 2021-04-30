package org.parchmentmc.compass.storage;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * An immutable {@link MappingDataContainer}.
 */
public class ImmutableMappingDataContainer implements MappingDataContainer {
    private final Set<MappingDataContainer.PackageData> packages = new TreeSet<>(PackageData.COMPARATOR);
    private final Collection<MappingDataContainer.PackageData> packagesView = Collections.unmodifiableSet(packages);
    private final Map<String, MappingDataContainer.PackageData> packagesMap = new HashMap<>();

    private final Set<MappingDataContainer.ClassData> classes = new TreeSet<>(ClassData.COMPARATOR);
    private final Collection<MappingDataContainer.ClassData> classesView = Collections.unmodifiableSet(classes);
    private final Map<String, MappingDataContainer.ClassData> classesMap = new HashMap<>();

    public ImmutableMappingDataContainer(Collection<? extends MappingDataContainer.PackageData> packages, Collection<? extends MappingDataContainer.ClassData> classes) {
        this.packages.addAll(packages);
        this.classes.addAll(classes);
        this.packagesMap.putAll(this.packages.stream().collect(Collectors.toMap(MappingDataContainer.PackageData::getName, Function.identity())));
        this.classesMap.putAll(this.classes.stream().collect(Collectors.toMap(MappingDataContainer.ClassData::getName, Function.identity())));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<? extends PackageData> getPackages() {
        return packagesView;
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public PackageData getPackage(String packageName) {
        return packagesMap.get(packageName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<? extends ClassData> getClasses() {
        return classesView;
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public ClassData getClass(String className) {
        return classesMap.get(className);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MappingDataContainer)) return false;
        MappingDataContainer builder = (MappingDataContainer) o;
        return getPackages().equals(builder.getPackages()) && getClasses().equals(builder.getClasses());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPackages(), getClasses());
    }

    /**
     * An immutable {@link MappingDataContainer.PackageData}.
     */
    public static class ImmutablePackageData implements MappingDataContainer.PackageData {
        private final String name;
        private final List<String> javadoc;

        public ImmutablePackageData(String name, List<String> javadoc) {
            this.name = name;
            this.javadoc = Collections.unmodifiableList(new ArrayList<>(javadoc));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getName() {
            return name;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<String> getJavadoc() {
            return javadoc;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PackageData)) return false;
            PackageData that = (PackageData) o;
            return Objects.equals(getName(), that.getName()) && getJavadoc().equals(that.getJavadoc());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getName(), getJavadoc());
        }
    }

    /**
     * An immutable {@link MappingDataContainer.ClassData}.
     */
    public static class ImmutableClassData implements MappingDataContainer.ClassData {
        private final String name;
        private final List<String> javadoc;
        private final Set<MappingDataContainer.FieldData> fields = new TreeSet<>(FieldData.COMPARATOR);
        private final Collection<MappingDataContainer.FieldData> fieldsView = Collections.unmodifiableSet(fields);
        private final Map<String, MappingDataContainer.FieldData> fieldsMap = new HashMap<>();

        private final Set<MappingDataContainer.MethodData> methods = new TreeSet<>(MethodData.COMPARATOR);
        private final Collection<MappingDataContainer.MethodData> methodsView = Collections.unmodifiableSet(methods);
        private final Map<String, MappingDataContainer.MethodData> methodsMap = new HashMap<>();

        public ImmutableClassData(String name, List<String> javadoc, Collection<? extends MappingDataContainer.FieldData> fields, Collection<? extends MappingDataContainer.MethodData> methods) {
            this.name = name;
            this.javadoc = Collections.unmodifiableList(new ArrayList<>(javadoc));
            this.fields.addAll(fields);
            this.fieldsMap.putAll(fields.stream().collect(Collectors.toMap(MappingDataContainer.FieldData::getName, Function.identity())));
            this.methods.addAll(methods);
            this.methodsMap.putAll(this.methods.stream().collect(Collectors.toMap(this::dataToKey, Function.identity())));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getName() {
            return name;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<String> getJavadoc() {
            return javadoc;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Collection<? extends MappingDataContainer.FieldData> getFields() {
            return fieldsView;
        }

        /**
         * {@inheritDoc}
         */
        @Nullable
        @Override
        public MappingDataContainer.FieldData getField(String fieldName) {
            return fieldsMap.get(fieldName);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Collection<? extends MappingDataContainer.MethodData> getMethods() {
            return methodsView;
        }

        /**
         * {@inheritDoc}
         */
        @Nullable
        @Override
        public MappingDataContainer.MethodData getMethod(String methodName, String descriptor) {
            return methodsMap.get(key(methodName, descriptor));
        }

        private String key(String name, String descriptor) {
            return name + ":" + descriptor;
        }

        private String dataToKey(MappingDataContainer.MethodData method) {
            return key(method.getName(), method.getDescriptor());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ClassData)) return false;
            ClassData that = (ClassData) o;
            return Objects.equals(getName(), that.getName()) && getJavadoc().equals(that.getJavadoc())
                    && getFields().equals(that.getFields()) && getMethods().equals(that.getMethods());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getName(), getJavadoc(), getFields(), getMethods());
        }
    }

    /**
     * An immutable {@link MappingDataContainer.FieldData}.
     */
    public static class ImmutableFieldData implements MappingDataContainer.FieldData {
        private final String name;
        private final String descriptor;
        private final List<String> javadoc;

        public ImmutableFieldData(String name, String descriptor, List<String> javadoc) {
            this.name = name;
            this.descriptor = descriptor;
            this.javadoc = Collections.unmodifiableList(new ArrayList<>(javadoc));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getName() {
            return name;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDescriptor() {
            return descriptor;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<String> getJavadoc() {
            return javadoc;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FieldData)) return false;
            FieldData that = (FieldData) o;
            return getName().equals(that.getName()) && Objects.equals(getDescriptor(), that.getDescriptor())
                    && getJavadoc().equals(that.getJavadoc());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getName(), getDescriptor(), getJavadoc());
        }
    }

    /**
     * An immutable {@link MappingDataContainer.MethodData}.
     */
    public static class ImmutableMethodData implements MappingDataContainer.MethodData {
        private final String name;
        private final String descriptor;
        private final List<String> javadoc;
        private final Set<MappingDataContainer.ParameterData> parameters = new TreeSet<>(ParameterData.COMPARATOR);
        private final Collection<MappingDataContainer.ParameterData> parametersView = Collections.unmodifiableSet(parameters);
        private final Map<Byte, MappingDataContainer.ParameterData> parametersMap = new HashMap<>();

        public ImmutableMethodData(String name, String descriptor, List<String> javadoc, Collection<? extends MappingDataContainer.ParameterData> parameters) {
            this.name = name;
            this.javadoc = Collections.unmodifiableList(new ArrayList<>(javadoc));
            this.descriptor = descriptor;
            this.parameters.addAll(parameters);
            this.parametersMap.putAll(this.parameters.stream().collect(Collectors.toMap(MappingDataContainer.ParameterData::getIndex, Function.identity())));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getName() {
            return name;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDescriptor() {
            return descriptor;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<String> getJavadoc() {
            return javadoc;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Collection<? extends MappingDataContainer.ParameterData> getParameters() {
            return parametersView;
        }

        /**
         * {@inheritDoc}
         */
        @Nullable
        @Override
        public MappingDataContainer.ParameterData getParameter(byte index) {
            return parametersMap.get(index);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MethodData)) return false;
            MethodData that = (MethodData) o;
            return getName().equals(that.getName()) && getDescriptor().equals(that.getDescriptor())
                    && getJavadoc().equals(that.getJavadoc()) && getParameters().equals(that.getParameters());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getName(), getDescriptor(), getJavadoc(), getParameters());
        }
    }

    /**
     * An immutable {@link MappingDataContainer.ParameterData}.
     */
    public static class ImmutableParameterData implements MappingDataContainer.ParameterData {
        private final byte index;
        @Nullable
        private final String name;
        @Nullable
        private final String javadoc;

        public ImmutableParameterData(byte index, @Nullable String name, @Nullable String javadoc) {
            this.index = index;
            this.name = name;
            this.javadoc = javadoc;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public byte getIndex() {
            return index;
        }

        /**
         * {@inheritDoc}
         */
        @Nullable
        @Override
        public String getName() {
            return name;
        }

        /**
         * {@inheritDoc}
         */
        @Nullable
        @Override
        public String getJavadoc() {
            return javadoc;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ParameterData)) return false;
            ParameterData that = (ParameterData) o;
            return getIndex() == that.getIndex() && Objects.equals(getName(), that.getName()) && Objects.equals(getJavadoc(), that.getJavadoc());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getIndex(), getName(), getJavadoc());
        }
    }
}
