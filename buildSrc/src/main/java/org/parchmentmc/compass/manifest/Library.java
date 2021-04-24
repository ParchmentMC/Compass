package org.parchmentmc.compass.manifest;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * A library used by a game version.
 */
public class Library implements Serializable {
    /**
     * The name of the library.
     *
     * <p>This is formatted similarly to an Apache Maven dependency, but only having three groups:
     * <tt><em>group</em>:<em>name</em>:<em>version</em></tt></p>
     */
    public String name;
    /**
     * Information for extracting the library into a folder, or {@code null} if the library will not be extracted.
     */
    @Nullable
    public ExtractInfo extract;
    /**
     * The downloads information for this library.
     */
    public LibraryDownloads downloads;
    /**
     * Information for the natives contained in this library, or {@code null} if no natives are contained.
     */
    @Nullable
    public NativesInfo natives;
    /**
     * The list of rules for this library.
     *
     * <p>If there are no rules specified for a library, it will default to allowing the library. If there is at least one
     * rule specified, then the library is denied unless allowed by a rule.</p>
     */
    public List<Rule> rules;

    /**
     * The information for the file downloads of the library.
     */
    public static class LibraryDownloads implements Serializable {
        /**
         * The download info for a single artifact file, or {@code null} if not specified.
         *
         * <p>If this is not specified, the {@link #classifiers} map usually has artifacts specified, and vice-versa.</p>
         */
        @Nullable
        public ArtifactDownload artifact;
        /**
         * A map of classifiers to their respective artifact download information.
         *
         * <p>If this is not specified, the {@link #artifact} is usually specified, and vice-versa.</p>
         */
        @Nullable
        public Map<String, ArtifactDownload> classifiers;
    }

    /**
     * The download information for a file for the library.
     */
    public static class ArtifactDownload extends VersionManifest.DownloadInfo {
        /**
         * A relative path for storing the downloaded file.
         *
         * <p>This is formatted similarly to how an Apache Maven artifact would be stored on disk:
         * <tt>(<em>group...</em>/)<em>name</em>/<em>version</em>/<em>name</em>-<em>version</em>[-<em>classifier</em>].<em>extension</em></tt>.</p>
         */
        public String path;
    }

    /**
     * Information used when extracting a library.
     */
    public static class ExtractInfo implements Serializable {
        /**
         * A list of paths within the library which is excluded from extraction.
         */
        public List<String> exclude;
    }

    /**
     * Information for the natives contained within this library.
     */
    public static class NativesInfo implements Serializable {
        /**
         * The classifier key that contains the natives for the Linux operating system.
         *
         * <p>Note that this classifier key may not have an entry in the library's downloads.</p>
         *
         * @see LibraryDownloads#classifiers
         */
        public String linux;

        /**
         * The classifier key that contains the natives for the OSX operating system.
         *
         * <p>Note that this classifier key may not have an entry in the library's downloads.</p>
         *
         * @see LibraryDownloads#classifiers
         */
        public String osx;
        /**
         * The classifier key that contains the natives for the Windows operating system.
         *
         * <p>Note that this classifier key may not have an entry in the library's downloads.</p>
         *
         * @see LibraryDownloads#classifiers
         */
        public String windows;
    }

    /**
     * A rule, used to add or remove libraries depending on conditions.
     *
     * <p>The action of the rule is applied if and only if the conditions of the rule are satisfied. If the rule
     * has no conditions specified, then it will always apply.</p>
     *
     * <p>If multiple rules are applied against a library, only the last one that has its conditions satisfied
     * will take effect.</p>
     */
    public static class Rule implements Serializable {
        /**
         * The action taken for the library, if the rule's conditions are satisfied.
         *
         * <p>Two values have been seen so far:
         * <dl>
         *     <dt><tt><strong>allow</strong></tt></dt>
         *     <dd>Use this library for the game.</dd>
         *     <dt><tt><strong>deny</strong></tt></dt>
         *     <dd>Do not use this library for the game.</dd>
         * </dl>
         * </p>
         */
        public String action;
        /**
         * The OS condition for this role, or {@code null} if this condition is not specified.
         */
        @Nullable
        public OSCondition os;

        /**
         * The rule condition matching against an OS.
         */
        public static class OSCondition implements Serializable {
            /**
             * The name of the OS that this rule matches against.
             */
            public String name;
            /**
             * A regex pattern which matches against the version of the OS.
             *
             * @see java.util.regex.Pattern
             */
            public String version;
        }
    }
}
