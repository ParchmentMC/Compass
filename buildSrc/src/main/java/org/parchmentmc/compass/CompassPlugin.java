package org.parchmentmc.compass;

import de.undercouch.gradle.tasks.download.Download;
import org.parchmentmc.compass.manifest.LauncherManifest;
import org.parchmentmc.compass.manifest.VersionManifest;
import org.parchmentmc.compass.tasks.DisplayMinecraftVersions;
import org.parchmentmc.compass.tasks.DownloadObfuscationMappings;
import org.parchmentmc.compass.tasks.DownloadVersionManifests;
import org.parchmentmc.compass.util.JSONUtil;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;

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

        // FIXME: remove after testing
        {
            TaskProvider<DownloadVersionManifests> download21w15aManifest = tasks.register("download21w15aManifest", DownloadVersionManifests.class);
            download21w15aManifest.configure(t -> {
                t.setGroup(COMPASS_GROUP);
                t.getManifest().set(manifest);
                t.getVersions().addAll("21w15a");
                t.quiet(true);
            });

            //noinspection NullableProblems
            Provider<VersionManifest> verManifest = download21w15aManifest.map(s -> s.getOutputFiles().get(0)).map(JSONUtil::tryParseVersionManifest);

            TaskProvider<DownloadObfuscationMappings> download21w15aMappings = tasks.register("download21w15aMappings", DownloadObfuscationMappings.class);
            download21w15aMappings.configure(t -> {
                t.setGroup(COMPASS_GROUP);
                t.getVersionManifest().set(verManifest);
            });

        }

    }
}
