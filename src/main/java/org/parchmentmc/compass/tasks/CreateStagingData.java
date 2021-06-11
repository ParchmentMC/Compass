package org.parchmentmc.compass.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.parchmentmc.compass.CompassExtension;
import org.parchmentmc.compass.CompassPlugin;
import org.parchmentmc.compass.storage.input.InputsReader;
import org.parchmentmc.compass.storage.io.ExplodedDataIO;
import org.parchmentmc.feather.mapping.MappingDataContainer;
import org.parchmentmc.feather.mapping.MappingUtil;

import java.io.IOException;

public abstract class CreateStagingData extends DefaultTask {
    public CreateStagingData() {
        CompassExtension extension = getProject().getExtensions().getByType(CompassExtension.class);

        getInputsDirectory().convention(extension.getInputs());
        getBaseDataDirectory().convention(extension.getProductionData());
        getOutputDirectory().convention(extension.getStagingData());
    }

    @TaskAction
    public void create() throws IOException {
        CompassPlugin plugin = getProject().getPlugins().getPlugin(CompassPlugin.class);
        InputsReader inputsReader = new InputsReader(plugin.getIntermediates());

        MappingDataContainer inputData = inputsReader.parse(getInputsDirectory().get().getAsFile().toPath());
        MappingDataContainer baseData = ExplodedDataIO.INSTANCE.read(getBaseDataDirectory().get().getAsFile());

        MappingDataContainer combinedData = MappingUtil.apply(baseData, inputData);

        ExplodedDataIO.INSTANCE.write(combinedData, getOutputDirectory().get().getAsFile());
    }

    @InputDirectory
    public abstract DirectoryProperty getInputsDirectory();

    @InputDirectory
    public abstract DirectoryProperty getBaseDataDirectory();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();
}
