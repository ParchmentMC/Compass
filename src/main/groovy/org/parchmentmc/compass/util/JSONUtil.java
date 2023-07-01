package org.parchmentmc.compass.util;

import com.squareup.moshi.*;
import okio.BufferedSource;
import okio.Okio;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.feather.io.moshi.*;
import org.parchmentmc.feather.manifests.LauncherManifest;
import org.parchmentmc.feather.manifests.VersionManifest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class JSONUtil {
    public static final Moshi MOSHI = new Moshi.Builder()
            .add(new OffsetDateTimeAdapter())
            .add(new MDCMoshiAdapter(true))
            .add(new SimpleVersionAdapter())
            .add(LinkedHashSetMoshiAdapter.FACTORY)
            .add(new MetadataMoshiAdapter())
            .build();

    private JSONUtil() {
    } // No instantiation of utilities

    @Nullable
    public static LauncherManifest parseLauncherManifest(Path manifest) throws IOException {
        return parseManifest(LauncherManifest.class, manifest);
    }

    @Nullable
    public static LauncherManifest tryParseLauncherManifest(Path manifest) {
        try {
            return parseLauncherManifest(manifest);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public static LauncherManifest tryParseLauncherManifest(File manifest) {
        return tryParseLauncherManifest(manifest.toPath());
    }

    @Nullable
    public static VersionManifest parseVersionManifest(Path manifest) throws IOException {
        return parseManifest(VersionManifest.class, manifest);
    }

    @Nullable
    public static VersionManifest tryParseVersionManifest(Path manifest) {
        try {
            return parseVersionManifest(manifest);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public static VersionManifest tryParseVersionManifest(File manifest) {
        return tryParseVersionManifest(manifest.toPath());
    }

    @Nullable
    private static <T> T parseManifest(Class<T> type, Path manifest) throws IOException {
        if (Files.notExists(manifest))
            throw new FileNotFoundException("Manifest file does not exist: " + manifest.toAbsolutePath());
        if (!Files.isRegularFile(manifest))
            throw new IllegalArgumentException("Not a regular file: " + manifest.toAbsolutePath());

        try (BufferedSource source = Okio.buffer(Okio.source(manifest))) {
            return MOSHI.adapter(type).fromJson(source);
        }
    }
}
