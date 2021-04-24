package org.parchmentmc.compass.manifest;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * The launcher manifest, version 2.
 *
 * <p>Retrievable from <tt>https://launchermeta.mojang.com/mc/game/version_manifest_v2.json</tt>.</p>
 *
 * @see <a href="https://minecraft.fandom.com/wiki/Version_manifest.json">Official Minecraft wiki, <tt>Version_manifest.json</tt></a>
 */
public class LauncherManifest implements Serializable {
    /**
     * The latest version information.
     */
    public LatestVersionInfo latest;
    /**
     * The list of known Minecraft versions.
     */
    public List<VersionData> versions;

    /**
     * The information for the latest release and snapshot versions.
     */
    public static class LatestVersionInfo implements Serializable {
        /**
         * The ID of the latest Minecraft release version.
         */
        public String release;
        /**
         * The ID of the latest Minecraft snapshot version.
         */
        public String snapshot;
    }

    /**
     * The version data for a specific Minecraft version.
     */
    public static class VersionData implements Serializable {
        /**
         * The ID of the Minecraft version associated with the data.
         */
        public String id;
        /**
         * The type of release for this version.
         *
         * <p>This is used by the launcher to categorize old or snapshot versions. As of 21w15a, there are 4 known values:
         * <dl>
         *     <dt><strong>{@code release}</strong></dt>
         *     <dd>A full release, such as {@code 1.16.5}.</dd>
         *
         *     <dt><strong>{@code snapshot}</strong></dt>
         *     <dd>An in-development snapshot, such as {@code 21w15a}.</dd>
         *
         *     <dt><strong>{@code old_beta}</strong></dt>
         *     <dd>An old <em>beta</em> version, such as {@code b1.8.1} (for version Beta 1.8.1).</dd>
         *
         *     <dt><strong>{@code old_alpha}</strong></dt>
         *     <dd>An old <em>alpha</em> version, such as {@code a1.2.6} (for version Alpha 1.2.6).</dd>
         * </dl>
         * </p>
         */
        public String type;
        /**
         * The URL where the manifest for this version can be downloaded from.
         *
         * <p>The current format is <tt>https://launchermeta.mojang.com/v1/packages/<strong>&lt;SHA-1 checksum of manifest></strong>/<strong><{@link #id}></strong>.json</tt></p>
         */
        public String url;
        /**
         * The (presumably) last date and time the manifest for the version was modified or updated.
         */
        public OffsetDateTime time;
        /**
         * The date and time when this version was publicly released.
         */
        public OffsetDateTime releaseTime;
        /**
         * The SHA-1 checksum of the manifest for this version.
         *
         * @see #url
         */
        public String sha1;
        /**
         * The safety compliance level of this version.
         *
         * <p>Used by the launcher to inform players if the version supports the most recent player safety features.</p>
         *
         * <p>Currently, all versions below <tt>1.16.4-pre1</tt> have a compliance level of 0, while versions above that
         * have a compliance level of 1.</p>
         */
        public int complianceLevel;
    }
}
