package org.parchmentmc.compass.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.parchmentmc.compass.storage.io.MappingIOFormat;
import org.parchmentmc.feather.mapping.MappingDataContainer;

import java.io.IOException;

public abstract class CopyData extends DefaultTask {
    public CopyData() {
        onlyIf(_t -> getInput().get().getAsFile().exists());
    }

    @InputDirectory
    public abstract DirectoryProperty getInput();

    @Input
    public abstract Property<MappingIOFormat> getInputFormat();

    @OutputDirectory
    public abstract DirectoryProperty getOutput();

    @Input
    public abstract Property<MappingIOFormat> getOutputFormat();

    @TaskAction
    public void move() throws IOException {
        MappingDataContainer staging = getInputFormat().get().read(getInput().get().getAsFile());

        getOutputFormat().get().write(staging, getOutput().get().getAsFile());
    }

}
