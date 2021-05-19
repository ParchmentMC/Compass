package org.parchmentmc.compass.util.download;

import de.undercouch.gradle.tasks.download.DownloadAction;
import org.gradle.api.Project;
import org.gradle.internal.hash.HashUtil;

import java.io.File;
import java.io.IOException;

public final class DownloadUtil {
    private DownloadUtil() {
    } // Prevent instantiation

    public static DownloadAction createAndExecuteAction(Project project, Object url, Object output, String info) throws IOException {
        DownloadAction action = new DownloadAction(project);
        action.quiet(true);
        action.overwrite(true); // Always check
        action.onlyIfModified(true); // Only re-download if changed
        action.useETag(true); // Use ETag to additionally check for changes
        action.src(url);
        action.dest(output);
        try {
            action.execute();
        } catch (IOException e) {
            throw new IOException("Exception while downloading " + info + " from " + action.getSrc(), e);
        }
        return action;
    }

    public static boolean areChecksumsEqual(File output, String expected) {
        return HashUtil.sha1(output).asZeroPaddedHexString(40).equals(expected);
    }

    public static boolean areNotChecksumsEqual(File output, String expected) {
        return !areChecksumsEqual(output, expected);
    }

    public static void verifyChecksum(File output, String expected, String info) throws IOException {
        String actual = HashUtil.sha1(output).asZeroPaddedHexString(40);
        if (!expected.equals(actual)) {
            throw new IOException("Hash for downloaded " + info +
                    " does not match expected; expected " + expected + ", actual is " + actual);
        }
    }
}
