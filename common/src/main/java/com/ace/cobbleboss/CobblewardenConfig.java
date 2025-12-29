package com.ace.cobbleboss;

/**
 * Configuration class for Cobblewarden Boss mod.
 * This contains all configurable values for the mod.
 */
public class CobblewardenConfig {
    
    /**
     * The HP that the Guzzlord boss should have.
     * Default: 500 (same as Warden)
     */
    public static final int GUZZLORD_HP = 500;
    
    /**
     * The level that the Guzzlord boss should spawn at.
     * Default: 75 (high level for strong stats)
     */
    public static final int GUZZLORD_LEVEL = 75;
    
    /**
     * The region size (in blocks) used to identify unique Ancient Cities.
     * Ancient Cities within this region will be considered the same city.
     * Default: 128 blocks (8 chunks)
     */
    public static final int ANCIENT_CITY_REGION_SIZE = 128;
    
    /**
     * Whether to show debug log messages.
     * Default: false
     */
    public static final boolean DEBUG_MODE = false;
}
