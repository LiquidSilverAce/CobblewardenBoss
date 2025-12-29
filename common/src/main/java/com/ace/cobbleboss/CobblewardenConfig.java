package com.ace.cobbleboss;

/**
 * Configuration class for Cobblewarden Boss mod.
 * This contains all configurable values for the mod.
 */
public class CobblewardenConfig {
    
    /**
     * The HP that the boss Pokemon should have.
     * Default: 500 (same as Warden)
     */
    public static final int BOSS_HP = 500;
    
    /**
     * The level that the boss Pokemon should spawn at.
     * Default: 100 (high level for maximum stats)
     */
    public static final int BOSS_LEVEL = 100;
    
    /**
     * The species to spawn in Ancient Cities.
     * Options: "guzzlord", "giratina_altered", "exploud"
     * Default: "giratina_altered"
     */
    public static final String ANCIENT_CITY_SPECIES = "giratina_altered";
    
    /**
     * The species to spawn outside Ancient Cities (overworld).
     * Options: "guzzlord", "giratina_altered", "exploud"
     * Default: "exploud"
     */
    public static final String OVERWORLD_SPECIES = "exploud";
    
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
    
    /**
     * The formatted display name for the boss.
     * Uses Minecraft formatting codes (§).
     */
    public static final String BOSS_NAME_FORMATTED = "§4Warden Boss§r";
    
    /**
     * The plain text name for the boss (without formatting codes).
     * Used for identification purposes.
     */
    public static final String BOSS_NAME_PLAIN = "Warden Boss";
    
    /**
     * Whether to prevent normal Pokemon battles (only allow Fight or Flight combat).
     * Default: true
     */
    public static final boolean PREVENT_NORMAL_BATTLE = true;
}
