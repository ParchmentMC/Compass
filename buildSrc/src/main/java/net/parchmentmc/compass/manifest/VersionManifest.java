package net.parchmentmc.compass.manifest;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * The manifest for a game version.
 *
 * @see <a href="https://minecraft.fandom.com/wiki/Client.json">Official Minecraft wiki, <tt>Client.json</tt></a>
 */
public class VersionManifest implements Serializable {
    /**
     * The ID of the Minecraft version associated with the manifest.
     *
     * @see LauncherManifest.VersionData#id
     */
    public String id;
    /**
     * The type of release for this version.
     *
     * @see LauncherManifest.VersionData#type
     */
    public String type;
    /**
     * The safety compliance level of this version.
     *
     * @see LauncherManifest.VersionData#complianceLevel
     */
    public int complianceLevel;
    /**
     * The minimum launcher version required to run this version.
     */
    public int minimumLauncherVersion;
    /**
     * The (presumably) last date and time the manifest for the version was modified or updated.
     * TODO: verify this.
     */
    public OffsetDateTime time;
    /**
     * The date and time when this version was publicly released.
     */
    public OffsetDateTime releaseTime;
    /**
     * The fully-qualified name of the main class for the game.
     */
    public String mainClass;
    /**
     * The java version on which this game runs on, or {@code null} if none is specified.
     */
    @Nullable
    public JavaVersionInfo javaVersionInfo;
    /**
     * The asset version ID used by this game version.
     */
    public String assets;
    /**
     * The information of the asset index used for this game version.
     */
    public AssetIndexInfo assetIndex;
    /**
     * A map of download keys and their corresponding information.
     *
     * <p>As of 21w15a, there are four known downloads:
     * <dl>
     *     <dt><strong><tt>client</tt></strong></dt>
     *     <dd>The client JAR. <em>Should always be available.</em></dd>
     *     <dt><strong><tt>server</tt></strong></dt>
     *     <dd>The dedicated server JAR.</dd>
     *     <dt><strong><tt>client_mappings</tt></strong></dt>
     *     <dd>The obfuscation map for the client JAR. </dd>
     *     <dt><strong><tt>server_mappings</tt></strong></dt>
     *     <dd>The obfuscation map for the dedicated server JAR.</dd>
     * </dl></p>
     */
    public Map<String, DownloadInfo> downloads;
    /**
     * The list of libraries for this game version.
     */
    public List<Library> libraries;
    /**
     * A string which is passed into the game as arguments, or {@code null} if not specified.
     *
     * <p>Tokens can appear in the string, in the form of <tt>{<em>token name</em>}</tt>. These will be replaced
     * by the launcher with certain information gathered from the running environment.</p>
     *
     * <p>Usually only used by old versions (old_alpha and old_beta).</p>
     */
    @Nullable
    public String minecraftArguments;

    // TODO: arguments[game,jvm], logging

    /**
     * Information for the java version which a game version runs on.
     */
    public static class JavaVersionInfo implements Serializable {
        public String component;
        public int majorVersion;
    }

    /**
     * Information for a download.
     */
    public static class DownloadInfo implements Serializable {
        /**
         * The SHA-1 checksum of the download.
         */
        public String sha1;
        /**
         * The size of the download in bytes.
         */
        public int size;
        /**
         * URL where the download is located at.
         */
        public String url;
    }

    public static class AssetIndexInfo extends DownloadInfo {
        public String id;
        public int totalSize;
    }
}
