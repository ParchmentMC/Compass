package org.parchmentmc.compass.tasks;

import de.undercouch.gradle.tasks.download.DownloadAction;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

abstract class BatchedDownload extends DefaultTask {
    protected List<Action<DownloadAction>> configure = new ArrayList<>();

    protected BatchedDownload() {
        configureDownload(action -> {
            action.quiet(true);
            action.overwrite(true); // Always check
            action.onlyIfModified(true); // Only re-download if changed
            action.useETag(true); // Use ETag to additionally check for changes
        });
    }

    @Internal
    protected abstract List<Pair<String, DownloadAction>> getDownloadActions();

    public void configureDownload(Action<DownloadAction> configure) {
        this.configure.add(Objects.requireNonNull(configure, "configure must not be null"));
    }

    protected DownloadAction createDownloadAction(Object src, Object dest) {
        DownloadAction action = new DownloadAction(getProject());
        action.src(src);
        action.dest(dest);
        return action;
    }

    @TaskAction
    public void download() {
        List<Pair<String, DownloadAction>> downloadActions = getDownloadActions();

        int upToDate = 0;

        for (Pair<String, DownloadAction> entry : downloadActions) {
            String name = entry.getLeft();
            DownloadAction action = Objects.requireNonNull(entry.getRight(), "Download action for " + name + " is null");
            try {
                configure.forEach(act -> act.execute(action));
                action.execute();
            } catch (IOException e) {
                throw new RuntimeException("Exception while downloading " + name, e);
            }
            if (action.isUpToDate()) {
                upToDate++;
            }
        }

        setDidWork(upToDate != downloadActions.size());
    }
}
