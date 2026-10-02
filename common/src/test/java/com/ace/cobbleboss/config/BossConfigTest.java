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
        BossConfig config = BossConfig.load(path);
        assertEquals("exploud", config.speciesFor(false, false));
        assertEquals("guzzlord", config.speciesFor(true, false));
        assertEquals(100, config.pokemonLevel());
        assertFalse(config.defaultUncatchable());
        assertFalse(config.defaultUnbattleable());
        assertTrue(config.ancientCityUncatchable());
        assertTrue(config.ancientCityUnbattleable());
        String json = Files.readString(path);
        assertTrue(json.contains("\"defaultUncatchable\""));
        assertTrue(json.contains("\"ancientCityUnbattleable\""));
        assertFalse(json.contains("\"bossRealTimeCombat\""));
    }

    @Test
    void missingSettingsKeepDefaults() {
        BossConfig config = BossConfig.parse("{\"defaultSpecies\":\"dusknoir\"}");
        assertTrue(config.enabled());
        assertTrue(config.ancientCityUncatchable());
        assertTrue(config.ancientCityUnbattleable());
        assertTrue(config.bossAlwaysAggressive());
        assertEquals(100, config.pokemonLevel());
        assertEquals("dusknoir", config.speciesFor(false, false));
        assertEquals("guzzlord", config.speciesFor(true, false));
    }

    @Test
    void legacyCityRestrictionsAreStillAccepted() {
        BossConfig config = BossConfig.parse("{\"bossUncatchable\":false,\"bossRealTimeCombat\":false,\"bossAlwaysAggressive\":false}");
        assertFalse(config.ancientCityUncatchable());
        assertFalse(config.ancientCityUnbattleable());
        assertFalse(config.bossAlwaysAggressive());
        assertTrue(BossConfig.parse("{\"bossUncatchable\":false}").ancientCityUnbattleable());
        assertFalse(config.defaultUncatchable());
        assertFalse(config.defaultUnbattleable());
    }

    @Test
    void newCitySettingsTakePrecedenceOverLegacyAliases() {
        BossConfig config = BossConfig.parse("""
                {"bossUncatchable": true, "bossRealTimeCombat": true,
                 "ancientCityUncatchable": false, "ancientCityUnbattleable": false}
                """);
        assertFalse(config.ancientCityUncatchable());
        assertFalse(config.ancientCityUnbattleable());
    }

    @Test
    void existingExplicitSpeciesAndLevelArePreserved() throws Exception {
        Path path = directory.resolve("cobblewarden_boss.json");
        String json = """
                {"defaultSpecies": "dusknoir", "ancientCitySpecies": "default", "pokemonLevel": 70,
                 "bossUncatchable": false, "bossRealTimeCombat": false}
                """;
        Files.writeString(path, json);
        BossConfig config = BossConfig.load(path);
        assertEquals("dusknoir", config.defaultSpecies());
        assertEquals("default", config.ancientCitySpecies());
        assertEquals(70, config.pokemonLevel());
        assertFalse(config.ancientCityUncatchable());
        assertFalse(config.ancientCityUnbattleable());
        assertEquals(json, Files.readString(path));
    }

    @Test
    void allFourRestrictionTogglesAreIndependent() {
        for (int flags = 0; flags < 16; flags++) {
            boolean regularCapture = (flags & 1) != 0, regularBattle = (flags & 2) != 0;
            boolean cityCapture = (flags & 4) != 0, cityBattle = (flags & 8) != 0;
            BossConfig config = BossConfig.parse("""
                    {"defaultUncatchable": %s, "defaultUnbattleable": %s,
                     "ancientCityUncatchable": %s, "ancientCityUnbattleable": %s}
                    """.formatted(regularCapture, regularBattle, cityCapture, cityBattle));
            assertEquals(regularCapture, config.uncatchableFor(false, false));
            assertEquals(regularBattle, config.unbattleableFor(false, false));
            assertEquals(cityCapture, config.uncatchableFor(true, false));
            assertEquals(cityBattle, config.unbattleableFor(true, false));
            assertEquals(regularCapture, config.uncatchableFor(true, true));
            assertEquals(regularBattle, config.unbattleableFor(true, true));
        }
    }

    @Test
    void defaultSpeciesInUnclearedCitiesStillUsesCityRestrictions() {
        BossConfig config = BossConfig.parse("{\"ancientCitySpecies\":\"default\"}");
        assertEquals("exploud", config.speciesFor(true, false));
        assertTrue(config.uncatchableFor(true, false));
        assertTrue(config.unbattleableFor(true, false));
        assertFalse(config.uncatchableFor(true, true));
        assertFalse(config.unbattleableFor(true, true));
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
            "{\"bossUncatchable\":null}", "{\"bossRealTimeCombat\":\"true\"}", "{\"bossAlwaysAggressive\":1}",
            "{\"defaultUncatchable\":null}", "{\"defaultUnbattleable\":\"true\"}",
            "{\"ancientCityUncatchable\":1}", "{\"ancientCityUnbattleable\":null}",
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
