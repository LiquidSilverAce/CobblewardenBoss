package com.ace.cobbleboss.fabric;

import com.ace.cobbleboss.CobblewardenBoss;
import net.fabricmc.api.ModInitializer;

public final class CobblewardenBossFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        CobblewardenBoss.init();
    }
}
