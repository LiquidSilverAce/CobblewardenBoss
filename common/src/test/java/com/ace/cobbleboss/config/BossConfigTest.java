package com.ace.cobbleboss.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class BossConfigTest {
    @TempDir Path directory;

    @Test
    void firstRunCreatesReloadableDefaults() throws Exception {
        Path path = directory.resolve("config/cobblewarden_boss.json");
        assertEquals(BossConfig.DEFAULT, BossConfig.load(path));
        assertEquals(BossConfig.DEFAULT, BossConfig.load(path));
        assertEquals("exploud", BossConfig.load(path).speciesFor(true, false));
    }

    @Test
    void missingSettingsKeepDefaults() {
        BossConfig config = BossConfig.parse("{\"defaultSpecies\":\"dusknoir\"}");
        assertTrue(config.enabled());
        assertEquals(70, config.pokemonLevel());
        assertEquals("dusknoir", config.speciesFor(false, false));
        assertEquals("dusknoir", config.speciesFor(true, false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"giratina", "guzzlord"})
    void cityBossesNeverEscapeCitiesAndDefeatedCitiesUseConfiguredFallback(String boss) {
        for (String fallback : new String[]{"exploud", "dusknoir"}) {
            BossConfig config = new BossConfig(true, fallback, boss, 70);
            assertEquals(fallback, config.speciesFor(false, false));
            assertEquals(fallback, config.speciesFor(false, true));
            assertEquals(boss, config.speciesFor(true, false));
            assertEquals(fallback, config.speciesFor(true, true));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "null", "[]", "{", "{\"defaultSpecies\":null}", "{\"defaultSpecies\":\"giratina\"}",
            "{\"defaultSpecies\":\"guzzlord\"}", "{\"ancientCitySpecies\":\"pikachu\"}",
            "{\"pokemonLevel\":0}", "{\"pokemonLevel\":101}", "{\"pokemonLevel\":70.5}",
            "{\"pokemonLevel\":4294967366}", "{\"pokemonLevel\":\"70\"}",
            "{\"enabled\":\"false\"}", "{\"defaultSpeces\":\"dusknoir\"}"
    })
    void invalidSettingsAreRejected(String json) {
        assertThrows(RuntimeException.class, () -> BossConfig.parse(json));
    }

    @Test
    void invalidConfigIsNotOverwrittenAndErrorIdentifiesFile() throws Exception {
        Path path = directory.resolve("cobblewarden_boss.json");
        String invalid = "{\"defaultSpecies\":\"giratina\"}";
        Files.writeString(path, invalid);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> BossConfig.load(path));
        assertTrue(error.getMessage().contains(path.toString()));
        assertEquals(invalid, Files.readString(path));
    }
}
