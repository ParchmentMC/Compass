package org.parchmentmc.compass.tasks;

import com.squareup.moshi.Moshi;
import net.minecraftforge.srgutils.IMappingFile;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.parchmentmc.compass.CompassExtension;
import org.parchmentmc.compass.CompassPlugin;
import org.parchmentmc.compass.providers.IntermediateProvider;
import org.parchmentmc.compass.storage.MappingDataContainer;
import org.parchmentmc.compass.storage.io.ExplodedDataIO;
import org.parchmentmc.compass.storage.io.MappingDataContainerAdapter;
import org.parchmentmc.compass.storage.io.SingleFileDataIO;
import org.parchmentmc.compass.util.MappingUtil;

import java.io.IOException;

public class GenerateExport extends DefaultTask {
    private static SingleFileDataIO IO = new SingleFileDataIO(new Moshi.Builder()
            .add(new MappingDataContainerAdapter(true)).build(), "  ");
    
    private final DirectoryProperty input;
    private final Property<String> intermediate;
    private final RegularFileProperty output;

    public GenerateExport() {
        ObjectFactory objects = getProject().getObjects();

        input = objects.directoryProperty();
        intermediate = objects.property(String.class);
        output = objects.fileProperty()
                .convention(getProject().getLayout().getBuildDirectory().dir(getName()).map(d -> d.file("export.json")));

        onlyIf(_t -> input.get().getAsFile().exists());
    }

    @TaskAction
    public void export() throws IOException {
        CompassPlugin plugin = getProject().getPlugins().getPlugin(CompassPlugin.class);

        IntermediateProvider intermediate = plugin.getIntermediates().getByName(this.intermediate.get());
        IMappingFile mapping = intermediate.getMapping();

        MappingDataContainer data = ExplodedDataIO.INSTANCE.read(input.get().getAsFile());

        MappingDataContainer remappedData = MappingUtil.remapData(data, mapping);

        IO.write(remappedData, output.get().getAsFile());
    }

    @InputDirectory
    public DirectoryProperty getInput() {
        return input;
    }

    @Input
    public Property<String> getIntermediate() {
        return intermediate;
    }

    @OutputFile
    public RegularFileProperty getOutput() {
        return output;
    }
}
