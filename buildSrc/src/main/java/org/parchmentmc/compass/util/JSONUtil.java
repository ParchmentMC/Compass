package org.parchmentmc.compass.util;

import com.squareup.moshi.*;
import org.parchmentmc.compass.manifest.LauncherManifest;
import org.parchmentmc.compass.manifest.VersionManifest;
import okio.BufferedSource;
import okio.Okio;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public final class JSONUtil {
    public static JsonAdapter<OffsetDateTime> OFFSET_DATE_TIME_ADAPTER = new OffsetDateTimeAdapter();
    private static final Moshi MOSHI = new Moshi.Builder()
            .add(OffsetDateTime.class, OFFSET_DATE_TIME_ADAPTER)
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

    static class OffsetDateTimeAdapter extends JsonAdapter<OffsetDateTime> {

        @Nullable
        @Override
        public OffsetDateTime fromJson(JsonReader reader) throws IOException {
            if (reader.peek() == JsonReader.Token.NULL) {
                throw new JsonDataException("Unexpected null at " + reader.getPath());
            } else {
                return OffsetDateTime.parse(reader.nextString());
            }
        }

        @Override
        public void toJson(JsonWriter writer, @Nullable OffsetDateTime value) throws IOException {
            if (value == null) {
                throw new JsonDataException("Unexpected null at " + writer.getPath());
            } else {
                writer.jsonValue(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value));
            }
        }
    }
}
