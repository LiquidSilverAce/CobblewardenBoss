package com.ace.cobbleboss.neoforge;

import net.neoforged.fml.common.Mod;

import com.ace.cobbleboss.CobblewardenBoss;

@Mod(CobblewardenBoss.MOD_ID)
public final class ExampleModNeoForge {
    public ExampleModNeoForge() {
        // Run our common setup.
        CobblewardenBoss.init();
    }
}
