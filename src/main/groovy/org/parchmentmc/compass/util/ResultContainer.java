package org.parchmentmc.compass.util;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * Container for arbitrary result data for packages, classes, methods, fields, and parameters.
 *
 * @param <T> the type of stored result data
 */
public class ResultContainer<T> {
    final Map<String, PackageResult<T>> packages = new TreeMap<>();
    final Map<String, ClassResult<T>> classes = new TreeMap<>();

    public ResultContainer() {
    }

    public void addPackage(PackageResult<T> pkg) {
        packages.put(pkg.getName(), pkg);
    }

    public void addClass(ClassResult<T> clz) {
        classes.put(clz.getName(), clz);
    }

    @Nullable
    public PackageResult<T> getPackage(String name) {
        return packages.get(name);
    }

    @Nullable
    public ClassResult<T> getClass(String name) {
        return classes.get(name);
    }

    public Collection<? extends PackageResult<T>> getPackages() {
        return packages.values();
    }

    public Collection<? extends ClassResult<T>> getClasses() {
        return classes.values();
    }

    public boolean isEmpty() {
        return getPackages().isEmpty() && getClasses().isEmpty();
    }

    abstract static class AbstractResult<T> {
        private final String name;
        final T data;

        protected AbstractResult(String name, T data) {
            this.name = name;
            this.data = data;
        }

        public String getName() {
            return name;
        }

        public T getData() {
            return data;
        }
    }

    public static class PackageResult<T> extends AbstractResult<T> {
        public PackageResult(String name, T data) {
            super(name, data);
        }
    }

    public static class ClassResult<T> extends AbstractResult<T> {
        final Map<String, FieldResult<T>> fields = new TreeMap<>();
        final Map<String, MethodResult<T>> methods = new TreeMap<>();

        public ClassResult(String name, T data) {
            super(name, data);
        }

        public void addField(FieldResult<T> field) {
            fields.put(field.getName(), field);
        }

        public void addMethod(MethodResult<T> method) {
            methods.put(key(method.getName(), method.getDescriptor()), method);
        }

        @Nullable
        public FieldResult<T> getField(String name) {
            return fields.get(name);
        }

        @Nullable
        public MethodResult<T> getMethod(String name, String descriptor) {
            return methods.get(key(name, descriptor));
        }

        public Collection<? extends FieldResult<T>> getFields() {
            return fields.values();
        }

        public Collection<? extends MethodResult<T>> getMethods() {
            return methods.values();
        }

        public boolean isEmpty() {
            return getFields().isEmpty() && getMethods().isEmpty();
        }

        private String key(String name, String descriptor) {
            return name + ":" + descriptor;
        }
    }

    public static class FieldResult<T> extends AbstractResult<T> {
        public FieldResult(String name, T data) {
            super(name, data);
        }
    }

    public static class MethodResult<T> extends AbstractResult<T> {
        private final String descriptor;
        private final Map<Byte, ParameterResult<T>> parameters = new TreeMap<>();

        public MethodResult(String name, String descriptor, T data) {
            super(name, data);
            this.descriptor = descriptor;
        }

        public String getDescriptor() {
            return descriptor;
        }

        public void addParameter(ParameterResult<T> parameter) {
            parameters.put(parameter.getIndex(), parameter);
        }

        @Nullable
        public ParameterResult<T> getParameter(byte index) {
            return parameters.get(index);
        }

        public boolean isEmpty() {
            return getParameters().isEmpty();
        }

        public Collection<? extends ParameterResult<T>> getParameters() {
            return parameters.values();
        }
    }

    public static class ParameterResult<T> {
        private final byte index;
        private final T data;

        public ParameterResult(byte index, T data) {
            this.index = index;
            this.data = data;
        }

        public byte getIndex() {
            return index;
        }

        public T getData() {
            return data;
        }
    }
}
