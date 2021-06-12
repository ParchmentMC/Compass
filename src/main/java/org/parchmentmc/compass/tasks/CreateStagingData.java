package org.parchmentmc.compass.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;
import org.parchmentmc.compass.CompassExtension;
import org.parchmentmc.compass.CompassPlugin;
import org.parchmentmc.compass.storage.input.InputsReader;
import org.parchmentmc.compass.storage.io.ExplodedDataIO;
import org.parchmentmc.feather.mapping.MappingDataBuilder;
import org.parchmentmc.feather.mapping.MappingDataContainer;

import java.io.IOException;

import static org.parchmentmc.feather.mapping.MappingDataBuilder.*;

public abstract class CreateStagingData extends DefaultTask {
    public CreateStagingData() {
        CompassExtension extension = getProject().getExtensions().getByType(CompassExtension.class);

        getInputsDirectory().convention(extension.getInputs());
        getBaseDataDirectory().convention(extension.getProductionData());
        getInputMode().convention(InputMode.OVERRIDE);
        getOutputDirectory().convention(extension.getStagingData());
    }

    @TaskAction
    public void create() throws IOException {
        CompassPlugin plugin = getProject().getPlugins().getPlugin(CompassPlugin.class);
        InputsReader inputsReader = new InputsReader(plugin.getIntermediates());

        MappingDataContainer inputData = inputsReader.parse(getInputsDirectory().get().getAsFile().toPath());
        MappingDataContainer baseData = ExplodedDataIO.INSTANCE.read(getBaseDataDirectory().get().getAsFile());

        MappingDataContainer combinedData = apply(baseData, inputData, getInputMode().get());

        ExplodedDataIO.INSTANCE.write(combinedData, getOutputDirectory().get().getAsFile());
    }

    @InputDirectory
    public abstract DirectoryProperty getInputsDirectory();

    @InputDirectory
    public abstract DirectoryProperty getBaseDataDirectory();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @Input
    public abstract Property<InputMode> getInputMode();

    @Option(option = "mode", description = "The operation mode for combining the input data with the base data.")
    public void setMode(InputMode mode) {
        getInputMode().set(mode);
    }

    public enum InputMode {
        OVERWRITE, // If present in input data, overwrites data in base (even if empty javadocs/name)
        OVERRIDE, // If present in input data and has javadocs/name, then overrides data in base
        ADDITIVE // Only adds from input data if javadocs/name is not set in base
    }

    // Copy of MappingUtil#apply but with input mode
    static MappingDataContainer apply(MappingDataContainer baseData, MappingDataContainer newData, InputMode mode) {
        if (newData.getClasses().isEmpty() && newData.getPackages().isEmpty()) return baseData;

        MappingDataBuilder builder = MappingDataBuilder.copyOf(baseData);

        builder.getPackages().forEach(pkg -> {
            final PackageData inputPkg = newData.getPackage(pkg.getName());

            if (inputPkg != null && ((mode == InputMode.OVERWRITE)
                    || (mode == InputMode.OVERRIDE && !inputPkg.getJavadoc().isEmpty())
                    || (mode == InputMode.ADDITIVE && pkg.getJavadoc().isEmpty()))) {
                pkg.addJavadoc(inputPkg.getJavadoc());
            }
        });

        builder.getClasses().forEach(cls -> {
            final ClassData inputCls = newData.getClass(cls.getName());

            if (inputCls != null) {
                if ((mode == InputMode.OVERWRITE)
                        || (mode == InputMode.OVERRIDE && !inputCls.getJavadoc().isEmpty())
                        || (mode == InputMode.ADDITIVE && cls.getJavadoc().isEmpty())) {
                    cls.addJavadoc(inputCls.getJavadoc());
                }

                cls.getFields().forEach(field -> {
                    final FieldData inputField = inputCls.getField(field.getName());

                    if (inputField != null && ((mode == InputMode.OVERWRITE)
                            || (mode == InputMode.OVERRIDE && !inputField.getJavadoc().isEmpty())
                            || (mode == InputMode.ADDITIVE && field.getJavadoc().isEmpty()))) {
                        field.addJavadoc(inputField.getJavadoc());
                    }
                });

                cls.getMethods().forEach(method -> {
                    final MethodData inputMethod = inputCls.getMethod(method.getName(), method.getDescriptor());

                    if (inputMethod != null) {
                        if ((mode == InputMode.OVERWRITE)
                                || (mode == InputMode.OVERRIDE && !inputMethod.getJavadoc().isEmpty())
                                || (mode == InputMode.ADDITIVE && method.getJavadoc().isEmpty())) {
                            method.addJavadoc(inputMethod.getJavadoc());
                        }

                        method.getParameters().forEach(param -> {
                            final ParameterData inputParam = inputMethod.getParameter(param.getIndex());

                            if (inputParam != null) {
                                if ((mode == InputMode.OVERWRITE)
                                        || (mode == InputMode.OVERRIDE && (inputParam.getName() != null))
                                        || (mode == InputMode.ADDITIVE && (param.getName() != null))) {
                                    param.setName(inputParam.getName());
                                }

                                if ((mode == InputMode.OVERWRITE)
                                        || (mode == InputMode.OVERRIDE && (inputParam.getJavadoc() != null))
                                        || (mode == InputMode.ADDITIVE && (param.getJavadoc() == null))) {
                                    param.setJavadoc(inputParam.getJavadoc());
                                }
                            }
                        });
                    }
                });
            }
        });

        return builder;
    }
}
