package org.parchmentmc.compass.providers.mcpconfig;

/**
 * Skeleton of a 'config.json' from a MCPConfig artifact.
 *
 * <p>This is deliberately kept as small as possible, to only have fields for the data which we need to process and to
 * reduce the maintenance and documentation burden.</p>
 */
class MCPConfigFile {
    /**
     * The specification version.
     *
     * <p>Known values are 1 (the original MCPConfig specification) and 2 (the new specification with class SRG IDs).</p>
     */
    int spec;
    /**
     * The Minecraft version targeted by this artifact.
     */
    String version;
    ConfigData data;

    /**
     * Locations of data files within the artifact.
     */
    static class ConfigData {
        /**
         * Location of the TSRG mapping file.
         */
        String mappings;
    }
}
