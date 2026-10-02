package com.ace.cobbleboss.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public record BossConfig(boolean enabled, String defaultSpecies, String ancientCitySpecies, int pokemonLevel,
                         boolean defaultUncatchable, boolean defaultUnbattleable,
                         boolean ancientCityUncatchable, boolean ancientCityUnbattleable, boolean bossAlwaysAggressive) {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> FIELDS = Set.of("enabled", "defaultSpecies", "ancientCitySpecies", "pokemonLevel",
            "defaultUncatchable", "defaultUnbattleable", "ancientCityUncatchable", "ancientCityUnbattleable",
            "bossUncatchable", "bossRealTimeCombat", "bossAlwaysAggressive");
    public static final BossConfig DEFAULT = new BossConfig(true, "exploud", "guzzlord", 100);

    public BossConfig(boolean enabled, String defaultSpecies, String ancientCitySpecies, int pokemonLevel) {
        this(enabled, defaultSpecies, ancientCitySpecies, pokemonLevel, false, false, true, true, true);
    }

    /** Compatibility with callers using the previous city-only settings. */
    public BossConfig(boolean enabled, String defaultSpecies, String ancientCitySpecies, int pokemonLevel,
                      boolean bossUncatchable, boolean bossRealTimeCombat, boolean bossAlwaysAggressive) {
        this(enabled, defaultSpecies, ancientCitySpecies, pokemonLevel,
                false, false, bossUncatchable, bossRealTimeCombat, bossAlwaysAggressive);
    }

    public BossConfig {
        if (defaultSpecies == null || !Set.of("exploud", "dusknoir").contains(defaultSpecies)) {
            throw new IllegalArgumentException("defaultSpecies must be exploud or dusknoir");
        }
        if (ancientCitySpecies == null || !Set.of("default", "giratina", "guzzlord").contains(ancientCitySpecies)) {
            throw new IllegalArgumentException("ancientCitySpecies must be default, giratina, or guzzlord");
        }
        if (pokemonLevel < 1 || pokemonLevel > 100) {
            throw new IllegalArgumentException("pokemonLevel must be between 1 and 100");
        }
    }

    public String speciesFor(boolean ancientCity, boolean bossDefeated) {
        return ancientCity && !bossDefeated && !ancientCitySpecies.equals("default")
                ? ancientCitySpecies : defaultSpecies;
    }

    public boolean uncatchableFor(boolean ancientCity, boolean bossDefeated) {
        return ancientCity && !bossDefeated ? ancientCityUncatchable : defaultUncatchable;
    }

    public boolean unbattleableFor(boolean ancientCity, boolean bossDefeated) {
        return ancientCity && !bossDefeated ? ancientCityUnbattleable : defaultUnbattleable;
    }

    public static BossConfig load(Path path) throws IOException {
        if (Files.notExists(path)) {
            Files.createDirectories(path.toAbsolutePath().getParent());
            Files.writeString(path, GSON.toJson(DEFAULT) + System.lineSeparator(), StandardCharsets.UTF_8);
            return DEFAULT;
        }
        try {
            return parse(Files.readString(path, StandardCharsets.UTF_8));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid configuration in " + path + ": " + exception.getMessage(), exception);
        }
    }

    static BossConfig parse(String json) {
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonObject()) {
            throw new IllegalArgumentException("Expected a JSON object");
        }
        JsonObject object = root.getAsJsonObject();
        for (String key : object.keySet()) {
            if (!FIELDS.contains(key)) {
                throw new IllegalArgumentException("Unknown setting: " + key);
            }
        }
        boolean enabled = DEFAULT.enabled;
        if (object.has("enabled")) {
            JsonElement value = object.get("enabled");
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
                throw new IllegalArgumentException("enabled must be a boolean");
            }
            enabled = value.getAsBoolean();
        }
        int level = DEFAULT.pokemonLevel;
        if (object.has("pokemonLevel")) {
            JsonElement value = object.get("pokemonLevel");
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
                throw new IllegalArgumentException("pokemonLevel must be an integer");
            }
            try {
                level = value.getAsBigDecimal().intValueExact();
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException("pokemonLevel must be an integer between 1 and 100", exception);
            }
        }
        return new BossConfig(enabled, string(object, "defaultSpecies", DEFAULT.defaultSpecies),
                string(object, "ancientCitySpecies", DEFAULT.ancientCitySpecies), level,
                bool(object, "defaultUncatchable", DEFAULT.defaultUncatchable),
                bool(object, "defaultUnbattleable", DEFAULT.defaultUnbattleable),
                bool(object, "ancientCityUncatchable", bool(object, "bossUncatchable", DEFAULT.ancientCityUncatchable)),
                bool(object, "ancientCityUnbattleable", bool(object, "bossRealTimeCombat", DEFAULT.ancientCityUnbattleable)),
                bool(object, "bossAlwaysAggressive", DEFAULT.bossAlwaysAggressive));
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        if (!object.has(key)) return fallback;
        JsonElement value = object.get(key);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException(key + " must be a boolean");
        }
        return value.getAsBoolean();
    }

    private static String string(JsonObject object, String key, String fallback) {
        if (!object.has(key)) {
            return fallback;
        }
        JsonElement value = object.get(key);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException(key + " must be a string");
        }
        return value.getAsString();
    }
}
