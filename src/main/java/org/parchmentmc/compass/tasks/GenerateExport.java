package org.parchmentmc.compass.tasks;

import com.squareup.moshi.Moshi;
import net.minecraftforge.srgutils.IMappingFile;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.parchmentmc.compass.CompassPlugin;
import org.parchmentmc.compass.providers.IntermediateProvider;
import org.parchmentmc.compass.storage.io.MappingIOFormat;
import org.parchmentmc.compass.storage.io.SingleFileDataIO;
import org.parchmentmc.compass.util.MappingUtil;
import org.parchmentmc.feather.io.moshi.MDCMoshiAdapter;
import org.parchmentmc.feather.io.moshi.SimpleVersionAdapter;
import org.parchmentmc.feather.mapping.MappingDataContainer;

import java.io.IOException;

public abstract class GenerateExport extends DefaultTask {
    private static final SingleFileDataIO IO = new SingleFileDataIO(new Moshi.Builder()
            .add(new MDCMoshiAdapter(true))
            .add(new SimpleVersionAdapter()).build(), "  ");

    public GenerateExport() {
        getOutput().convention(getProject().getLayout().getBuildDirectory().dir(getName()).map(d -> d.file("export.json")));

        onlyIf(_t -> getInput().get().getAsFile().exists());
    }

    @TaskAction
    public void export() throws IOException {
        CompassPlugin plugin = getProject().getPlugins().getPlugin(CompassPlugin.class);
        IMappingFile officialMap = plugin.getObfuscationMapsDownloader().getObfuscationMap().get(); // moj -> obf

        IntermediateProvider intermediate = plugin.getIntermediates().getByName(getIntermediate().get());
        IMappingFile mapping = intermediate.getMapping(); // obf -> ?
        IMappingFile officialToIntermediate = officialMap.chain(mapping); // [moj -> obf] -> [obf -> ?] => moj -> ?

        MappingDataContainer data = getInputFormat().get().read(getInput().get().getAsFile());

        MappingDataContainer remappedData = MappingUtil.remapData(data, officialToIntermediate);

        MappingDataContainer output = modifyData(remappedData);

        IO.write(output, getOutput().get().getAsFile());
    }

    protected MappingDataContainer modifyData(MappingDataContainer container) throws IOException {
        return container;
    }

    @InputDirectory
    public abstract DirectoryProperty getInput();

    @Input
    public abstract Property<MappingIOFormat> getInputFormat();

    @Input
    public abstract Property<String> getIntermediate();

    @OutputFile
    public abstract RegularFileProperty getOutput();
}
