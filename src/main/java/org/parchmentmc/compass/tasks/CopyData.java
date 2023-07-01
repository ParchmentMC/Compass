package org.parchmentmc.compass.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.parchmentmc.compass.storage.io.MappingIOFormat;
import org.parchmentmc.feather.mapping.MappingDataContainer;

import java.io.File;
import java.io.IOException;

public abstract class CopyData extends DefaultTask {
    @InputDirectory
    @Optional
    public abstract DirectoryProperty getInputDirectory();

    @InputFile
    @Optional
    public abstract RegularFileProperty getInputFile();

    @Input
    public abstract Property<MappingIOFormat> getInputFormat();

    @OutputDirectory
    @Optional
    public abstract DirectoryProperty getOutputDirectory();

    @OutputFile
    @Optional
    public abstract RegularFileProperty getOutputFile();

    @Input
    public abstract Property<MappingIOFormat> getOutputFormat();

    @TaskAction
    public void move() throws IOException {
        MappingDataContainer staging = getInputFormat().get().read(getFile("input", getInputDirectory(), getInputFile()));

        getOutputFormat().get().write(staging, getFile("output", getOutputDirectory(), getOutputFile()));
    }

    private File getFile(String type, DirectoryProperty dir, RegularFileProperty file) {
        if (!dir.isPresent() && !file.isPresent()) {
            throw new InvalidUserDataException("Neither file or directory " + type + " property has a value");
        }
        if (dir.isPresent() && file.isPresent()) {
            throw new InvalidUserDataException("Both file and directory " + type + " properties have values");
        }

        if (dir.isPresent()) return dir.get().getAsFile();
        return file.get().getAsFile();
    }
}
