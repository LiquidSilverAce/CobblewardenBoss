# Cobblewarden Boss

A Minecraft **1.21.1 / Java 21** addon for Cobblemon on Fabric and NeoForge. Sculk shriekers summon level **100 Exploud** instead of a Warden by default, or level **100 Guzzlord** for the first boss in each Ancient City. You can select **Dusknoir** for regular spawns or **Giratina** for city bosses instead.

## Configuration

Start the server or open a single-player world once to create `config/cobblewarden_boss.json`:

```json
{
  "enabled": true,
  "defaultSpecies": "exploud",
  "ancientCitySpecies": "guzzlord",
  "pokemonLevel": 100,
  "defaultUncatchable": false,
  "defaultUnbattleable": false,
  "ancientCityUncatchable": true,
  "ancientCityUnbattleable": true,
  "bossAlwaysAggressive": true
}
```

| Setting | Allowed values | Behavior |
| --- | --- | --- |
| `enabled` | `true`, `false` | `false` restores vanilla Warden spawning. |
| `defaultSpecies` | `exploud`, `dusknoir` | Used outside Ancient Cities and in cities whose boss was defeated. |
| `ancientCitySpecies` | `default`, `giratina`, `guzzlord` | `default` uses `defaultSpecies` everywhere. Otherwise, an uncleared city uses the selected boss. |
| `pokemonLevel` | Integer from 1 to 100 (default: `100`) | Level of all replacements. Cobblemon's own maximum level still applies. |
| `defaultUncatchable` | `true`, `false` (default) | Prevent capture of regular replacements, including spawns in cleared cities. |
| `defaultUnbattleable` | `true`, `false` (default) | Block turn-based Pokémon battles for regular replacements, including forced encounters. |
| `ancientCityUncatchable` | `true` (default), `false` | Prevent capture of replacements summoned in uncleared Ancient Cities. |
| `ancientCityUnbattleable` | `true` (default), `false` | Block turn-based Pokémon battles for replacements summoned in uncleared Ancient Cities, including forced encounters. Works with or without Fight or Flight Reborn. |
| `bossAlwaysAggressive` | `true` (default), `false` | With Reborn installed, force city bosses to attack the triggering player, falling back to the nearest eligible player. |

For Dusknoir in ordinary areas and Giratina in uncleared cities, set `defaultSpecies` to `dusknoir` and `ancientCitySpecies` to `giratina`. Use `guzzlord` for Guzzlord instead. Restart the server or reopen the single-player world after editing. Missing settings retain their defaults; invalid values stop startup with a message identifying the configuration file, without overwriting it.

## Boss combat

The four capture/battle toggles are independent and affect only wild Pokémon summoned by this addon. Regular replacements use `defaultUncatchable` and `defaultUnbattleable`; replacements summoned in uncleared Ancient Cities use `ancientCityUncatchable` and `ancientCityUnbattleable`, even when `ancientCitySpecies` is `default`. After a city's boss is defeated, new replacements there use the regular settings. Each spawned Pokémon keeps its encounter category through saves and chunk reloads. Existing marked Giratina/Guzzlord city bosses also receive the city settings when loaded. Ordinary wild Pokémon and player-owned Pokémon retain their existing behavior. `bossAlwaysAggressive` still affects only Giratina/Guzzlord city bosses with Reborn installed.

Older config keys remain supported: `bossUncatchable` supplies `ancientCityUncatchable`, and `bossRealTimeCombat` supplies `ancientCityUnbattleable`. An explicit new key takes precedence over its old alias. Existing explicit species and level values are preserved; change `pokemonLevel` to `100` and `ancientCitySpecies` to `guzzlord` in an existing file to adopt the new defaults. Newly generated files include all four toggles.

Install [Fight or Flight Reborn 0.11.0](https://modrinth.com/mod/cobblemon-fight-or-flight-reborn) and its dependencies on the server and clients for open-world combat. With the defaults, city bosses cannot be captured or challenged to a regular turn-based battle. Send out your Pokémon to fight them using Reborn's combat mechanics. The addon overrides Reborn's normal boss aggression/flee decisions and prevents its forced battle encounters for these bosses; it does not change Reborn's global config, damage balance, or controls.

The player responsible for the shriek has priority over closer players. If that player disconnects, dies, leaves the dimension, enters Creative/Spectator, or moves more than 64 blocks away, the boss targets the nearest living, non-invulnerable player within 64 blocks. It resumes targeting the triggering player when that player becomes eligible again. Trigger identity survives entity saves and chunk reloads; older bosses without that identity use the nearest player.

Set a category's `Unbattleable` toggle to `false` to permit regular Pokémon battles, or its `Uncatchable` toggle to `false` to remove this addon's capture protection. Disabling `bossAlwaysAggressive` restores Reborn's normal targeting rules. Other mods' capture restrictions, such as Reborn's aggressive-Pokémon capture setting, still apply. Capture and battle restrictions work without Reborn; forced aggression requires Reborn. To use turn-based battles for city encounters without Reborn, set `ancientCityUnbattleable` to `false`.

## Ancient City progression

Cities are identified by the bounds and starting chunk of the actual `minecraft:ancient_city` structure, including the dimension. Being in the Deep Dark biome alone does not qualify. A boss belongs to the city containing the triggering shrieker, even if it later moves elsewhere.

When a Giratina or Guzzlord spawned by this mod faints in a Pokémon battle or is defeated by world damage, **only its own city** is marked cleared. Future triggers there use the current `defaultSpecies` setting. Changing the city boss from Giratina to Guzzlord does not reset cleared cities. Other cities still have their first boss encounter.

Completion is stored in the world's `data/cobblewarden_boss_cities.dat` and survives saving, chunk unloading, and server restarts. Spawning, despawning, fleeing, and catching a boss do not count as defeating it. A captured boss fainting later in your party does not clear a city. Existing spawned bosses are not converted when another boss is defeated.

## Spawn behavior

- Natural shriekers retain the four-warning requirement, cooldowns, sound responses, and darkness effect.
- `can_summon`, Peaceful difficulty, and `doWardenSpawning` retain their vanilla behavior. The separate `doMobSpawning` rule does not prevent these triggered spawns.
- Replacements use vanilla's 20 placement attempts in an 11 × 13 × 11 search area. Positions must have a suitable floor and enough room for the Pokémon's actual hitbox, and must be inside the world border, loaded chunks, and build limits.
- A nearby wild replacement blocks additional warnings and spawns in vanilla's 48-block-wide detection box. Simultaneous shriekers also check immediately before spawning.
- If no position fits, nothing spawns. The mod does not spawn a Warden or an unrelated random Pokémon as a fallback.
- `/summon`, spawn eggs, and existing Wardens are unaffected. Only shrieker-triggered spawning is replaced.
- Regular Exploud and Dusknoir can be battled and caught by default. Each encounter category uses its configured capture and battle restrictions. Replacements retain Cobblemon despawning and do not gain Warden melee attacks or sonic booms.

## Installation and compatibility

Use the jar for your loader from `fabric/build/libs/` or `neoforge/build/libs/`; do not install the `common`, `sources`, or `dev-shadow` jars.

Install it on the server, or in your client for single-player. It adds no client assets or custom networking, so a dedicated server does not require clients to install this addon. Players still need matching Cobblemon and Pokémon resource packs.

Required dependencies:

- Minecraft **1.21.1** and Java **21**.
- Cobblemon **1.7.3** or **1.8.1**.
- Architectury API **13.0.8** or newer for Minecraft 1.21.1.
- Fabric: Fabric Loader **0.18.4** or newer and Fabric API.
- NeoForge: NeoForge **21.1.215** or newer and the Kotlin for Forge dependencies required by your Cobblemon installation.

[Cobbleverse 1.7.42](https://modrinth.com/modpack/cobbleverse/version/4SKGla61) is a Fabric modpack containing Cobblemon **1.7.3**. That Cobblemon version is the default build target. Giratina and Guzzlord need an installed pack that supplies their models; Cobbleverse supplies additional Pokémon content. The addon uses the installed species and forms rather than bundling or replacing those assets.

Cobbleverse also blacklists `minecraft:warden` in MobsBeGone. Keep that blacklist: this addon intercepts the shrieker before a Warden entity is created and adds a Cobblemon Pokémon directly. Disabling this addon restores the pack's existing behavior, including its Warden blacklist.

## Build and verification

```sh
./gradlew build
./gradlew :fabric:runGametest
```

Build and run the same checks against Cobblemon 1.8.1:

```sh
./gradlew build :fabric:runGametest -Pcobblemon_version=1.8.1+1.21.1
```

Exercise Cobbleverse's exact MobsBeGone 0.0.7 release with Wardens blacklisted:

```sh
./gradlew :fabric:runGametest -PtestMobsBeGone
```

Exercise Fight or Flight Reborn 0.11.0 with either supported Cobblemon version:

```sh
./gradlew :fabric:runGametest -PtestFightOrFlight
./gradlew :fabric:runGametest -PtestFightOrFlight -Pcobblemon_version=1.8.1+1.21.1
```

These optional dependencies are only for the development runtime and are not bundled or required by the addon.

Unit tests cover config creation, validation, legacy aliases, all 16 combinations of the four restriction toggles, restricted species selection, and post-defeat selection. The Fabric GameTest runs in an isolated dedicated server and exercises real shrieker mixins, all four species, level-100 Guzzlord defaults, warning thresholds, gamerules, Peaceful, player-placed shriekers, obstructed placement, nearby bosses, vanilla opt-out, structure detection, independent city defeats, saved-data serialization, independent capture and battle restrictions, trigger-player priority, nearest-player fallback, and Reborn world damage. GameTest code is not included in release jars.

Verified locally for the configurable capture/battle settings:

| Check | Result |
| --- | --- |
| Configuration unit tests | 31 passed |
| Fabric and NeoForge release builds against Cobblemon 1.7.3 | Passed |
| Fabric dedicated-server GameTest with Cobblemon 1.7.3 | Passed |
| Fabric GameTest with Fight or Flight Reborn 0.11.0 on Cobblemon 1.7.3 | Passed |

The existing CI matrix also checks Cobblemon 1.8.1, Fight or Flight Reborn 0.11.0, and MobsBeGone 0.0.7 with Wardens blacklisted.

NeoForge gameplay and the complete Cobbleverse client/modpack have not been run. These checks exercise spawning, faint events, server AI ticks, battle cancellation, and open-world damage directly; a full interactive client combat encounter and model rendering still need an in-game check.

For a manual check in a test world, use Survival mode on a non-Peaceful difficulty with `/gamerule doWardenSpawning true`. Naturally generated shriekers work, or place a test one with `/setblock ~ ~ ~ minecraft:sculk_shrieker[can_summon=true]`. Give it enough open space for the selected Pokémon. Trigger four warnings, allowing at least ten seconds between them. A shrieker placed normally from an item cannot summon. Use `/locate structure minecraft:ancient_city` to test city encounters, defeat its boss, save and restart, and trigger again to check the fallback.

## Audit findings addressed

The original repository contained only the Architectury example template: empty initialization and mixin lists, no Cobblemon dependency, and no spawn or configuration code. The Gradle wrapper jar was missing and its Unix launcher was not executable. This implementation adds the shared behavior for both loaders, required dependency declarations, validated configuration, persistent city progression, regression coverage, a complete wrapper, and accurate mod metadata.
