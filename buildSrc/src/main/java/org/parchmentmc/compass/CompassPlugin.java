package org.parchmentmc.compass;

import de.undercouch.gradle.tasks.download.Download;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.parchmentmc.compass.manifest.LauncherManifest;
import org.parchmentmc.compass.manifest.VersionManifest;
import org.parchmentmc.compass.tasks.DisplayMinecraftVersions;
import org.parchmentmc.compass.tasks.DownloadObfuscationMaps;
import org.parchmentmc.compass.tasks.DownloadVersionManifest;
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
            t.src(extension.getLauncherManifestURL());
            t.dest(t.getProject().getLayout().getBuildDirectory().dir(t.getName()).map(d -> d.file("manifest.json").getAsFile()));
            t.overwrite(true);
            t.onlyIfModified(true);
            t.useETag(true);
            t.quiet(true);
        });

        //noinspection NullableProblems
        final Provider<LauncherManifest> launcherManifest = downloadLauncherManifest
                .map(Download::getDest)
                .map(JSONUtil::tryParseLauncherManifest);

        final TaskProvider<DisplayMinecraftVersions> displayMinecraftVersions = tasks.register("displayMinecraftVersions", DisplayMinecraftVersions.class);
        displayMinecraftVersions.configure(t -> {
            t.setGroup(COMPASS_GROUP);
            t.setDescription("Displays all known Minecraft versions.");
            t.getManifest().set(launcherManifest);
        });

        final Provider<LauncherManifest.VersionData> versionData = launcherManifest.zip(extension.getVersion(),
                (mf, ver) -> mf.versions.stream()
                        .filter(str -> str.id.equals(ver))
                        .findFirst()
                        .orElseThrow(() -> new InvalidUserDataException("No version data found for " + ver)));

        TaskProvider<DownloadVersionManifest> downloadVersionManifest = tasks.register("downloadVersionManifest", DownloadVersionManifest.class);
        downloadVersionManifest.configure(t -> {
            t.setGroup(COMPASS_GROUP);
            t.setDescription("Downloads the version manifest.");
            t.getVersionData().set(versionData);
            t.getVersion().set(extension.getVersion());
        });

        //noinspection NullableProblems
        final Provider<VersionManifest> versionManifest = downloadVersionManifest
                .flatMap(DownloadVersionManifest::getOutput)
                .map(RegularFile::getAsFile)
                .map(JSONUtil::tryParseVersionManifest);

        TaskProvider<DownloadObfuscationMaps> downloadObfuscationMaps = tasks.register("downloadObfuscationMaps", DownloadObfuscationMaps.class);
        downloadObfuscationMaps.configure(t -> {
            t.setGroup(COMPASS_GROUP);
            t.setDescription("Downloads the client and server obfuscation maps.");
            t.getVersionManifest().set(versionManifest);
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
