package org.parchmentmc.compass;

import de.undercouch.gradle.tasks.download.Download;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.parchmentmc.compass.manifest.LauncherManifest;
import org.parchmentmc.compass.tasks.DisplayMinecraftVersions;
import org.parchmentmc.compass.tasks.DownloadObfuscationMaps;
import org.parchmentmc.compass.tasks.DownloadVersionManifests;
import org.parchmentmc.compass.util.JSONUtil;

public class CompassPlugin implements Plugin<Project> {
    public static final String COMPASS_GROUP = "compass";

    @Override
    public void apply(Project project) {
        project.getPlugins().apply("de.undercouch.download");
        final CompassExtension extension = project.getExtensions().create("compass", CompassExtension.class, project);
        final TaskContainer tasks = project.getTasks();

        final TaskProvider<Download> downloadLauncherManifest = tasks.register("downloadLauncherManifest", Download.class);
        downloadLauncherManifest.configure(t -> {
            t.setGroup(COMPASS_GROUP);
            t.setDescription("Downloads the launcher manifest.");
            t.src(extension.getManifestURL());
            t.dest(t.getProject().getLayout().getBuildDirectory().dir("launcherManifest").map(d -> d.file("manifest.json").getAsFile()));
            t.overwrite(true);
            t.onlyIfModified(true);
            t.useETag(true);
            t.quiet(true);
        });

        //noinspection NullableProblems
        final Provider<LauncherManifest> manifest = downloadLauncherManifest.map(Download::getDest).map(JSONUtil::tryParseLauncherManifest);

        final TaskProvider<DisplayMinecraftVersions> displayMinecraftVersions = tasks.register("displayMinecraftVersions", DisplayMinecraftVersions.class);
        displayMinecraftVersions.configure(t -> {
            t.setGroup(COMPASS_GROUP);
            t.setDescription("Displays all known Minecraft versions.");
            t.getManifest().set(manifest);
        });

        TaskProvider<DownloadVersionManifests> downloadVersionManifests = tasks.register("downloadVersionManifests", DownloadVersionManifests.class);
        downloadVersionManifests.configure(t -> {
            t.setGroup(COMPASS_GROUP);
            t.setDescription("Download the version manifests for each version.");
            t.getLauncherManifest().convention(manifest);
            t.getVersionStorage().convention(extension.getVersionStorage());
            t.getVersions().convention(extension.getVersions());
            t.getOutputFileName().convention(extension.getVersionManifest());
        });

        TaskProvider<DownloadObfuscationMaps> downloadObfuscationMappings = tasks.register("downloadObfuscationMappings", DownloadObfuscationMaps.class);
        downloadObfuscationMappings.configure(t -> {
            t.setGroup(COMPASS_GROUP);
            t.dependsOn(downloadVersionManifests);
            t.setDescription("Download the obfuscation mappings for each version.");
            t.getVersionStorage().convention(extension.getVersionStorage());
            t.getVersions().convention(extension.getVersions());
            t.getVersionManifestFilename().convention(downloadVersionManifests.flatMap(DownloadVersionManifests::getOutputFileName));
        });

        /*
        t.doLast(_t -> {
            try {
                IMappingFile map = IMappingFile.load(t.getClientMappings().get().getAsFile());
                // getMapped() == obfuscated, getOriginal() == mojmap

                File file = project.file("versions/21w15a/data.json");
                JsonAdapter<StorageFile> adapter = JSONUtil.MOSHI.adapter(StorageFile.class).indent("\t");
                StorageFile storedFile = new StorageFile("21w15a");

                for (IMappingFile.IClass cls : map.getClasses()) {
                    int idx = cls.getOriginal().lastIndexOf('/');
                    String name;
                    if (idx == -1) {
                        name = cls.getOriginal();
                    } else {
                        name = cls.getOriginal().substring(0, idx);
                    }
                    StorageFile.StoredPackage storedPackage = new StorageFile.StoredPackage();
//                                storedPackage.addJavadoc("package " + name);
                    storedFile.addPackage(name, storedPackage);

                    StorageFile.StoredClass storedClass = new StorageFile.StoredClass();
//                                storedClass.addJavadoc("class " + cls.getMapped() + " [" + cls.getOriginal() + "]");
                    storedFile.addClass(cls.getMapped(), storedClass);

                    for (IMappingFile.IField field : cls.getFields()) {
                        StorageFile.StoredField storedField = new StorageFile.StoredField();
//                                    storedField.addJavadoc(String.format("class %s [%s]; field %s [%s]", cls.getMapped(), cls.getOriginal(), field.getMapped(), field.getOriginal()));
                        storedClass.addField(field.getMapped(), storedField);
                    }

                    for (IMappingFile.IMethod method : cls.getMethods()) {
                        StorageFile.StoredMethod storedMethod = new StorageFile.StoredMethod();
//                                    storedMethod.addJavadoc(String.format("class %s [%s]; method %s [%s] %s", cls.getMapped(), cls.getOriginal(), method.getMapped(), method.getOriginal(), method.getDescriptor()));
                        storedClass.addMethod(method.getMapped(), storedMethod);

                        Map<Integer, String> paramMap = SignatureHelper.countParameters(method.getMappedDescriptor());
//                                    paramMap.forEach(storedMethod::addParam);
                    }

                }

                if (!file.exists()) {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }
                try (BufferedSink sink = Okio.buffer(Okio.sink(file))) {
                    adapter.toJson(sink, storedFile);
                }

            } catch (IOException e) {
                throw new AssertionError(e);
            }
        });
        */

    }
}
