package org.parchmentmc.compass.util;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;

import java.io.Serializable;
import java.util.Objects;

/**
 * A version, represented by three version numbers.
 */
public final class SimpleVersion implements Serializable, Comparable<SimpleVersion> {
    private final int major;
    private final int minor;
    private final int patch;

    public SimpleVersion(String version) {
        String[] split = version.split("\\.");
        if (split.length < 2) throw new IllegalArgumentException("Expected at least 2 tokens for version: " + version);
        if (split.length > 3) throw new IllegalArgumentException("Expected at most 3 tokens for version: " + version);
        major = Integer.parseInt(split[0]);
        minor = Integer.parseInt(split[1]);
        patch = split.length == 3 ? Integer.parseInt(split[2]) : 0;
    }

    public SimpleVersion(int major, int minor, int patch) {
        if (major < 0) throw new IllegalArgumentException("Major version must not be negative: " + major);
        if (minor < 0) throw new IllegalArgumentException("Minor version must not be negative: " + minor);
        if (patch < 0) throw new IllegalArgumentException("Patch version must not be negative: " + patch);
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public SimpleVersion(SimpleVersion o) {
        this(o.major, o.minor, o.patch);
    }

    /**
     * Returns the major version number.
     *
     * <p>The major version number is incremented for any backwards-compatible changes, such as removals in the public
     * API or specification.</p>
     *
     * @return the major version number
     */
    public int getMajor() {
        return major;
    }

    /**
     * Returns the minor version number.
     *
     * <p>The minor version number is incremented for any notable backwards-compatible changes, such as additions
     * in the public API or specification.</p>
     *
     * @return the minor version number
     */
    public int getMinor() {
        return minor;
    }

    /**
     * Returns the patch version number.
     *
     * <p>The patch version number is incremented for any non-noteworthy backwards-compatible changes, such as fixes for
     * bugs or issues.</p>
     *
     * @return the patch version number
     */
    public int getPatch() {
        return patch;
    }

    /**
     * Returns whether the given version is compatible with this version.
     *
     * <p>Two versions are compatible if they have the same major version, ignoring the minor and patch versions.</p>
     *
     * @param o the other version to compare
     * @return if the given version is compatible
     * @see #getMajor()
     */
    public boolean isCompatibleWith(SimpleVersion o) {
        return this.major == o.major;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }

    @Override
    public int compareTo(SimpleVersion o) {
        int ret = this.major - o.major;
        if (ret != 0) return ret;
        ret = this.minor - o.minor;
        if (ret != 0) return ret;
        return this.patch - o.patch;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleVersion that = (SimpleVersion) o;
        return major == that.major && minor == that.minor && patch == that.patch;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch);
    }

    public static final class Adapter {
        @ToJson
        String toJson(SimpleVersion version) {
            return version.toString();
        }

        @FromJson
        SimpleVersion fromJson(String json) {
            return new SimpleVersion(json);
        }
    }
}
