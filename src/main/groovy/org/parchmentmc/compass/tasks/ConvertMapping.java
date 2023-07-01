package org.parchmentmc.compass.tasks;

import net.minecraftforge.srgutils.IMappingFile;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.File;
import java.io.IOException;

public abstract class ConvertMapping extends DefaultTask {
    @InputFile
    public abstract RegularFileProperty getInputFile();

    // Not used, but may be used in the future.
    @Input
    public abstract Property<Format> getInputFormat();

    @Input
    public abstract Property<Format> getOutputFormat();

    @Input
    @Optional
    public abstract Property<Boolean> getReverse();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    @TaskAction
    public void convert() throws IOException {
        final File input = getInputFile().get().getAsFile();
        final File output = getOutputFile().get().getAsFile();
        final Format inputFormat = getInputFormat().get();
        final Format outputFormat = getOutputFormat().get();
        final boolean reverse = getReverse().getOrElse(false);

        IMappingFile data = IMappingFile.load(input);
        data.write(output.toPath(), outputFormat.format, reverse);
    }

    public enum Format {
        SRG(IMappingFile.Format.SRG),
        XSRG(IMappingFile.Format.XSRG),
        CSRG(IMappingFile.Format.CSRG),
        TSRG(IMappingFile.Format.TSRG),
        TSRG2(IMappingFile.Format.TSRG2),
        PROGUARD(IMappingFile.Format.PG),
        TINY(IMappingFile.Format.TINY1),
        TINY2(IMappingFile.Format.TINY);

        final IMappingFile.Format format;

        Format(IMappingFile.Format format) {
            this.format = format;
        }
    }
}
