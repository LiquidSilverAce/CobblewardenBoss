package com.ace.cobbleboss;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CobblewardenBoss {
    public static final String MOD_ID = "cobblewarden_boss";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        LOGGER.info("Initializing Cobblewarden Boss mod");
        
        // Register event listeners
        WardenSpawnInterceptor.register();
        BossDefeatHandler.register();
        
        LOGGER.info("Cobblewarden Boss mod initialized");
    }
}
