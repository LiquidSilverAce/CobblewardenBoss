package com.ace.cobbleboss;

import com.ace.cobbleboss.config.BossConfig;
import com.ace.cobbleboss.combat.BossCombat;
import com.ace.cobbleboss.spawn.BossSpawner;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

public final class CobblewardenBoss {
    public static final String MOD_ID = "cobblewarden_boss";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static BossConfig config = BossConfig.DEFAULT;

    private CobblewardenBoss() {}

    public static BossConfig config() {
        return config;
    }

    public static void init() {
        LifecycleEvent.SERVER_BEFORE_START.register(server -> reloadConfig());
        CobblemonEvents.BATTLE_STARTED_PRE.subscribe(BossCombat::preventBattle);
        // World damage and ordinary battles both update Pokémon HP. The battle event also
        // supplies the original Pokémon when another addon battles with a temporary copy.
        CobblemonEvents.POKEMON_FAINTED.subscribe(event -> BossSpawner.recordDefeat(event.getPokemon()));
        CobblemonEvents.BATTLE_FAINTED.subscribe(event -> BossSpawner.recordDefeat(event.getKilled().getOriginalPokemon()));
    }

    public static void reloadConfig() {
        Path path = Platform.getConfigFolder().resolve(MOD_ID + ".json");
        try {
            config = BossConfig.load(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load " + path, exception);
        }
        LOGGER.info("Warden replacement: enabled={}, default={}, ancient city={}",
                config.enabled(), config.defaultSpecies(), config.ancientCitySpecies());
    }
}
