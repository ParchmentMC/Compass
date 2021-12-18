package org.parchmentmc.compass.util.download;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import de.undercouch.gradle.tasks.download.DownloadAction;
import org.gradle.api.Project;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

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

    @SuppressWarnings({"deprecation", "UnstableApiUsage"})
    private static String sha1(File file) {
        try {
            // While Hashing#sha1 is deprecated in favor of SHA-256, we have to use it as the hashes we use
            // are in SHA-1
            return Files.asByteSource(file).hash(Hashing.sha1()).toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to hash " + file.getAbsolutePath(), e);
        }
    }

    public static boolean areChecksumsEqual(File output, String expected) {
        return sha1(output).equals(expected);
    }

    public static boolean areNotChecksumsEqual(File output, String expected) {
        return !areChecksumsEqual(output, expected);
    }

    public static void verifyChecksum(File output, String expected, String info) throws IOException {
        String actual = sha1(output);
        if (!expected.equals(actual)) {
            throw new IOException("Hash for downloaded " + info +
                    " does not match expected; expected " + expected + ", actual is " + actual);
        }
    }
}
