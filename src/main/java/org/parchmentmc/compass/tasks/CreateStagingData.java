package org.parchmentmc.compass.tasks;

import net.minecraftforge.srgutils.IMappingFile;
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
import org.parchmentmc.compass.storage.io.MappingIOFormat;
import org.parchmentmc.compass.util.MappingUtil;
import org.parchmentmc.feather.mapping.MappingDataBuilder;
import org.parchmentmc.feather.mapping.MappingDataContainer;

import java.io.IOException;

import static org.parchmentmc.feather.mapping.MappingDataBuilder.*;

public abstract class CreateStagingData extends DefaultTask {
    public CreateStagingData() {
        CompassExtension extension = getProject().getExtensions().getByType(CompassExtension.class);

        getInputsDirectory().convention(extension.getInputs());
        getBaseDataDirectory().convention(extension.getProductionData());
        getBaseDataFormat().convention(extension.getProductionDataFormat());
        getInputMode().convention(InputMode.OVERRIDE);
        getOutputDirectory().convention(extension.getStagingData());
        getOutputFormat().convention(extension.getStagingDataFormat());
    }

    @TaskAction
    public void create() throws IOException {
        CompassPlugin plugin = getProject().getPlugins().getPlugin(CompassPlugin.class);
        InputsReader inputsReader = new InputsReader(plugin.getIntermediates());
        final IMappingFile officialMap = plugin.getObfuscationMapsDownloader().getObfuscationMap().get();
        /*
         * Three parts:
         *  - the base official data (`data`)
         *  - the base task data (`baseTaskData`)
         *  - the input data (`inputData`)
         *
         * First, apply the base task data to the base official data
         * Second, apply the input data to the combined data from the last step
         * Third, write out the data from the last step, ignoring undocumented
         */

        MappingDataBuilder data = MappingUtil.loadOfficialData(officialMap);
        MappingDataContainer baseTaskData = getBaseDataFormat().get().read(getBaseDataDirectory().get().getAsFile());
        MappingDataContainer inputData = inputsReader.parse(getInputsDirectory().get().getAsFile().toPath());

        apply(data, baseTaskData, InputMode.OVERWRITE);
        apply(data, inputData, getInputMode().get());

        getOutputFormat().get().write(data, getOutputDirectory().get().getAsFile());
    }

    @InputDirectory
    public abstract DirectoryProperty getInputsDirectory();

    @InputDirectory
    public abstract DirectoryProperty getBaseDataDirectory();

    @Input
    public abstract Property<MappingIOFormat> getBaseDataFormat();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @Input
    public abstract Property<MappingIOFormat> getOutputFormat();

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
    static void apply(MappingDataBuilder baseData, MappingDataContainer newData, InputMode mode) {
        if (newData.getClasses().isEmpty() && newData.getPackages().isEmpty()) return;

        baseData.getPackages().forEach(pkg -> {
            final PackageData inputPkg = newData.getPackage(pkg.getName());

            if (inputPkg != null && ((mode == InputMode.OVERWRITE)
                    || (mode == InputMode.OVERRIDE && !inputPkg.getJavadoc().isEmpty())
                    || (mode == InputMode.ADDITIVE && pkg.getJavadoc().isEmpty()))) {
                pkg.clearJavadoc().addJavadoc(inputPkg.getJavadoc());
            }
        });

        baseData.getClasses().forEach(cls -> {
            final ClassData inputCls = newData.getClass(cls.getName());

            if (inputCls != null) {
                if ((mode == InputMode.OVERWRITE)
                        || (mode == InputMode.OVERRIDE && !inputCls.getJavadoc().isEmpty())
                        || (mode == InputMode.ADDITIVE && cls.getJavadoc().isEmpty())) {
                    cls.clearJavadoc().addJavadoc(inputCls.getJavadoc());
                }

                cls.getFields().forEach(field -> {
                    final FieldData inputField = inputCls.getField(field.getName());

                    if (inputField != null && ((mode == InputMode.OVERWRITE)
                            || (mode == InputMode.OVERRIDE && !inputField.getJavadoc().isEmpty())
                            || (mode == InputMode.ADDITIVE && field.getJavadoc().isEmpty()))) {
                        field.clearJavadoc().addJavadoc(inputField.getJavadoc());
                    }
                });

                cls.getMethods().forEach(method -> {
                    final MethodData inputMethod = inputCls.getMethod(method.getName(), method.getDescriptor());

                    if (inputMethod != null) {
                        if ((mode == InputMode.OVERWRITE)
                                || (mode == InputMode.OVERRIDE && !inputMethod.getJavadoc().isEmpty())
                                || (mode == InputMode.ADDITIVE && method.getJavadoc().isEmpty())) {
                            method.clearJavadoc().addJavadoc(inputMethod.getJavadoc());
                        }

                        if (mode == InputMode.OVERWRITE) {
                            inputMethod.getParameters().forEach(param ->
                                    method.getOrCreateParameter(param.getIndex())
                                            .setName(param.getName())
                                            .setJavadoc(param.getJavadoc()));
                        } else {
                            method.getParameters().forEach(param -> {
                                final ParameterData inputParam = inputMethod.getParameter(param.getIndex());

                                if (inputParam != null) {
                                    if ((mode == InputMode.OVERRIDE && inputParam.getName() != null)
                                            || (mode == InputMode.ADDITIVE && param.getName() != null)) {
                                        param.setName(inputParam.getName());
                                    }

                                    if ((mode == InputMode.OVERRIDE && inputParam.getJavadoc() != null)
                                            || (mode == InputMode.ADDITIVE && param.getJavadoc() != null)) {
                                        param.setJavadoc(inputParam.getJavadoc());
                                    }
                                }
                            });

                            inputMethod.getParameters().forEach(inputParam -> {
                                final MutableParameterData param = method.getParameter(inputParam.getIndex());

                                if (param == null) { // New parameter
                                    method.createParameter(inputParam.getIndex())
                                            .setName(inputParam.getName())
                                            .setJavadoc(inputParam.getJavadoc());
                                }
                            });
                        }
                    }
                });
            }
        });
    }
}
