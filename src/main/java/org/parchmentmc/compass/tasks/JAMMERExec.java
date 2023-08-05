package org.parchmentmc.compass.tasks;

import org.gradle.api.Action;
import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputFile;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class JAMMERExec extends JavaExec {
    private final Provider<RegularFile> outputMapping;

    @Nested
    public abstract NamedDomainObjectContainer<IdentifiedVersion> getExistingVersions();

    @Nested
    public abstract Property<Version> getTargetVersion();

    public Version targetVersion(String version) {
        final Version ver = getObjects().newInstance(Version.class, version);
        getTargetVersion().set(ver);
        return ver;
    }

    public void targetVersion(String version, Action<? super Version> action) {
        action.execute(targetVersion(version));
    }

    @Internal("Represented as part of outputMapping")
    public abstract DirectoryProperty getOutputDirectory();

    @OutputFile
    public Provider<RegularFile> getOutputMapping() {
        return outputMapping;
    }

    @Inject
    protected abstract ObjectFactory getObjects();

    @Inject
    public JAMMERExec(final ProjectLayout layout) {
        // Always run the task
        getOutputs().upToDateWhen(s -> false);

        getMainClass().set("org.parchmentmc.jam.Main");

        getOutputDirectory().convention(layout.getBuildDirectory().dir(getName()));
        outputMapping = getObjects().fileProperty()
                .convention(getOutputDirectory().file("output.json"));

        getArgumentProviders().add(() -> Arrays.asList("--outputPath", getOutputDirectory().get().getAsFile().getAbsolutePath()));
        getArgumentProviders().add(() -> {
            List<String> arguments = new ArrayList<>();
            getExistingVersions().forEach(ver -> arguments.addAll(Arrays.asList(
                    "--existingNames", ver.getName(),
                    "--existingJars", ver.getJar().get().getAsFile().getAbsolutePath(),
                    "--existingMappings", ver.getMappings().get().getAsFile().getAbsolutePath(),
                    "--existingMetadata", ver.getMetadata().get().getAsFile().getAbsolutePath(),
                    "--existingIdentifiers", ver.getIdentifiers().get().getAsFile().getAbsolutePath()
            )));
            return arguments;
        });
        getArgumentProviders().add(() -> {
            final Version targetVer = getTargetVersion().get();
            return Arrays.asList(
                    "--inputName", targetVer.getName(),
                    "--inputJar", targetVer.getJar().get().getAsFile().getAbsolutePath(),
                    "--inputMapping", targetVer.getMappings().get().getAsFile().getAbsolutePath(),
                    "--inputMetadata", targetVer.getMetadata().get().getAsFile().getAbsolutePath()
            );
        });

        doFirst(t -> {
            if (getExistingVersions().size() <= 0)
                throw new RuntimeException("There should be at least one existing version");
        });
    }

    public interface Version extends Named {
        @Override
        @Input
        @NotNull
        String getName();

        @InputFile
        RegularFileProperty getJar();

        @InputFile
        RegularFileProperty getMappings();

        @InputFile
        RegularFileProperty getMetadata();
    }

    public interface IdentifiedVersion extends Version {
        @InputFile
        RegularFileProperty getIdentifiers();
    }
}
