package org.parchmentmc.compass.storage.io;

import com.squareup.moshi.*;
import org.parchmentmc.compass.storage.ImmutableMappingDataContainer;
import org.parchmentmc.compass.storage.MappingDataContainer;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MappingDataContainerAdapter {
    /* ****************** Serialization ****************** */

    @ToJson
    void containerToJson(JsonWriter writer,
                         MappingDataContainer container,
                         JsonAdapter<Collection<? extends MappingDataContainer.PackageData>> packageAdapter,
                         JsonAdapter<Collection<? extends MappingDataContainer.ClassData>> classAdapter) throws IOException {
        writer.beginObject();
        writer.name("packages").jsonValue(packageAdapter.toJsonValue(container.getPackages()));
        writer.name("classes").jsonValue(classAdapter.toJsonValue(container.getClasses()));
        writer.endObject();
    }

    @ToJson
    void packageToJson(JsonWriter writer,
                       MappingDataContainer.PackageData packageData,
                       JsonAdapter<List<String>> stringListAdapter) throws IOException {
        writer.beginObject()
                .name("name").value(packageData.getName());
        if (!packageData.getJavadoc().isEmpty())
            writer.name("javadoc").jsonValue(stringListAdapter.toJsonValue(packageData.getJavadoc()));
        writer.endObject();
    }

    @ToJson
    void classToJson(JsonWriter writer,
                     MappingDataContainer.ClassData classData,
                     JsonAdapter<List<String>> stringListAdapter,
                     JsonAdapter<Collection<? extends MappingDataContainer.FieldData>> fieldAdapter,
                     JsonAdapter<Collection<? extends MappingDataContainer.MethodData>> methodAdapter) throws IOException {
        writer.beginObject()
                .name("name").value(classData.getName());
        if (!classData.getJavadoc().isEmpty())
            writer.name("javadoc").jsonValue(stringListAdapter.toJsonValue(classData.getJavadoc()));
        if (!classData.getFields().isEmpty())
            writer.name("fields").jsonValue(fieldAdapter.toJsonValue(classData.getFields()));
        if (!classData.getMethods().isEmpty())
            writer.name("methods").jsonValue(methodAdapter.toJsonValue(classData.getMethods()));
        writer.endObject();
    }

    @ToJson
    void fieldToJson(JsonWriter writer,
                     MappingDataContainer.FieldData fieldData,
                     JsonAdapter<List<String>> stringListAdapter) throws IOException {
        writer.beginObject()
                .name("name").value(fieldData.getName())
                .name("descriptor").value(fieldData.getDescriptor());
        if (!fieldData.getJavadoc().isEmpty())
            writer.name("javadoc").jsonValue(stringListAdapter.toJsonValue(fieldData.getJavadoc()));
        writer.endObject();
    }

    @ToJson
    void methodToJson(JsonWriter writer,
                      MappingDataContainer.MethodData methodData,
                      JsonAdapter<List<String>> stringListAdapter,
                      JsonAdapter<Collection<? extends MappingDataContainer.ParameterData>> paramAdapter) throws IOException {
        writer.beginObject()
                .name("name").value(methodData.getName())
                .name("descriptor").value(methodData.getDescriptor());
        if (!methodData.getJavadoc().isEmpty())
            writer.name("javadoc").jsonValue(stringListAdapter.toJsonValue(methodData.getJavadoc()));
        if (!methodData.getParameters().isEmpty())
            writer.name("parameters").jsonValue(paramAdapter.toJsonValue(methodData.getParameters()));
        writer.endObject();
    }

    @ToJson
    void paramToJson(JsonWriter writer,
                     MappingDataContainer.ParameterData paramData) throws IOException {
        writer.beginObject()
                .name("index").value(paramData.getIndex());
        if (paramData.getName() != null)
            writer.name("name").value(paramData.getName());
        if (paramData.getJavadoc() != null)
            writer.name("javadoc").value(paramData.getJavadoc());
        writer.endObject();
    }

    /* ***************** Deserialization ***************** */

    @FromJson
    MappingDataContainer containerToJson(JsonReader reader,
                                         JsonAdapter<Collection<? extends MappingDataContainer.PackageData>> packageAdapter,
                                         JsonAdapter<Collection<? extends MappingDataContainer.ClassData>> classAdapter) throws IOException {


        Collection<? extends MappingDataContainer.PackageData> packages = Collections.emptyList();
        Collection<? extends MappingDataContainer.ClassData> classes = Collections.emptyList();

        reader.beginObject();
        while (reader.hasNext()) {
            String propertyName = reader.nextName();
            switch (propertyName) {
                case "packages":
                    packages = packageAdapter.fromJson(reader);
                    break;
                case "classes":
                    classes = classAdapter.fromJson(reader);
                    break;
                default:
                    reader.skipName();
                    break;
            }
        }
        reader.endObject();

        return new ImmutableMappingDataContainer(packages, classes);
    }

    @FromJson
    MappingDataContainer.PackageData packageFromJson(JsonReader reader,
                                                     JsonAdapter<List<String>> stringListAdapter) throws IOException {

        String name = null;
        List<String> javadoc = Collections.emptyList();

        reader.beginObject();
        while (reader.hasNext()) {
            String propertyName = reader.nextName();
            switch (propertyName) {
                case "name":
                    name = reader.nextString();
                    break;
                case "javadoc":
                    javadoc = stringListAdapter.fromJson(reader);
                    break;
                default:
                    reader.skipName();
                    break;
            }
        }
        reader.endObject();

        if (name == null) throw new IllegalArgumentException("Package name must not be null");

        return new ImmutableMappingDataContainer.ImmutablePackageData(name, javadoc);
    }


    @FromJson
    MappingDataContainer.ClassData classFromJson(JsonReader reader,
                                                 JsonAdapter<List<String>> stringListAdapter,
                                                 JsonAdapter<Collection<? extends MappingDataContainer.FieldData>> fieldAdapter,
                                                 JsonAdapter<Collection<? extends MappingDataContainer.MethodData>> methodAdapter) throws IOException {
        String name = null;
        List<String> javadoc = Collections.emptyList();
        Collection<? extends MappingDataContainer.FieldData> fields = Collections.emptyList();
        Collection<? extends MappingDataContainer.MethodData> methods = Collections.emptyList();

        reader.beginObject();
        while (reader.hasNext()) {
            String propertyName = reader.nextName();
            switch (propertyName) {
                case "name":
                    name = reader.nextString();
                    break;
                case "javadoc":
                    javadoc = stringListAdapter.fromJson(reader);
                    break;
                case "fields":
                    fields = fieldAdapter.fromJson(reader);
                    break;
                case "methods":
                    methods = methodAdapter.fromJson(reader);
                    break;
                default:
                    reader.skipName();
                    break;
            }
        }
        reader.endObject();

        if (name == null) throw new IllegalArgumentException("Class name must not be null");

        return new ImmutableMappingDataContainer.ImmutableClassData(name, javadoc, fields, methods);
    }

    @FromJson
    MappingDataContainer.FieldData fieldFromJson(JsonReader reader,
                                                 JsonAdapter<List<String>> stringListAdapter) throws IOException {
        String name = null;
        String descriptor = null;
        List<String> javadoc = Collections.emptyList();

        reader.beginObject();
        while (reader.hasNext()) {
            String propertyName = reader.nextName();
            switch (propertyName) {
                case "name":
                    name = reader.nextString();
                    break;
                case "descriptor":
                    descriptor = reader.nextString();
                    break;
                case "javadoc":
                    javadoc = stringListAdapter.fromJson(reader);
                    break;
                default:
                    reader.skipName();
                    break;
            }
        }
        reader.endObject();

        if (name == null) throw new IllegalArgumentException("Field name must not be null");
        if (descriptor == null) throw new IllegalArgumentException("Field descriptor must not be null");

        return new ImmutableMappingDataContainer.ImmutableFieldData(name, descriptor, javadoc);
    }

    @FromJson
    MappingDataContainer.MethodData methodFromJson(JsonReader reader,
                                                   JsonAdapter<List<String>> stringListAdapter,
                                                   JsonAdapter<Collection<? extends MappingDataContainer.ParameterData>> paramAdapter) throws IOException {
        String name = null;
        String descriptor = null;
        List<String> javadoc = Collections.emptyList();
        Collection<? extends MappingDataContainer.ParameterData> parameters = Collections.emptyList();

        reader.beginObject();
        while (reader.hasNext()) {
            String propertyName = reader.nextName();
            switch (propertyName) {
                case "name":
                    name = reader.nextString();
                    break;
                case "descriptor":
                    descriptor = reader.nextString();
                    break;
                case "javadoc":
                    javadoc = stringListAdapter.fromJson(reader);
                    break;
                case "parameters":
                    parameters = paramAdapter.fromJson(reader);
                    break;
                default:
                    reader.skipName();
                    break;
            }
        }
        reader.endObject();

        if (name == null) throw new IllegalArgumentException("Method name must not be null");
        if (descriptor == null) throw new IllegalArgumentException("Method descriptor must not be null");

        return new ImmutableMappingDataContainer.ImmutableMethodData(name, descriptor, javadoc, parameters);
    }

    @FromJson
    MappingDataContainer.ParameterData paramToJson(JsonReader reader) throws IOException {

        byte index = -1;
        String name = null;
        String javadoc = null;

        reader.beginObject();
        while (reader.hasNext()) {
            String propertyName = reader.nextName();
            switch (propertyName) {
                case "index":
                    index = (byte) reader.nextInt();
                    break;
                case "name":
                    name = reader.nextString();
                    break;
                case "javadoc":
                    javadoc = reader.nextString();
                    break;
                default:
                    reader.skipName();
                    break;
            }
        }
        reader.endObject();

        if (index < 0) throw new IllegalArgumentException("Parameter index must be present and positive");

        return new ImmutableMappingDataContainer.ImmutableParameterData(index, name, javadoc);
    }
}
