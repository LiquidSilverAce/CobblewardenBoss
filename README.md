# Cobblewarden Boss

A Minecraft mod for Cobbleverse 1.7.1-CF (Minecraft 1.21.1) that replaces the vanilla Warden with powerful boss Pokemon in both Ancient Cities and the overworld.

## Features

### Warden Replacement
- Intercepts vanilla Warden spawns
- Spawns a powerful boss Pokemon instead based on location
- Uses the same spawn triggers (sculk shrieker warnings, etc.)
- Maintains vanilla Warden spawn conditions

### Location-Based Spawning
- **Ancient Cities (Deep Dark biome)**: Spawns Giratina (Altered Forme) by default
- **Overworld**: Spawns Exploud by default
- Fully configurable - choose between Guzzlord, Giratina (Altered), or Exploud for each location

### Boss Pokemon Configuration
- **Aggressive Behavior**: Boss Pokemon attack players' Pokémon using Fight or Flight Reborn mechanics
- **Boosted Stats**: 
  - 500 HP (Warden-equivalent)
  - Level 100 with maximized IVs
  - Optimized EVs for HP and Attack
- **Uncatchable**: Cannot be captured with any Poké Ball
- **Boss Identity**: Custom name tag displaying "§4Warden Boss§r"
- **Battle Restriction**: Prevents normal Pokemon battles - players must use Fight or Flight Reborn's partner combat system

### Defeat Behavior
- Boss Pokemon despawn upon defeat (similar to Warden)
- Does not drop any loot or items
- No death animation, simply disappears

### Per-Structure Cooldown
- Each Ancient City can only spawn one boss Pokemon total
- **Exploud (Overworld)**: Uses same cooldown as vanilla Warden (can spawn multiple times)
- Cooldown is persistent across:
  - Server restarts
  - Different players
  - World reloads
- Uses structure bounding box detection to identify unique locations
- Data saved in world save files

## Configuration

Edit the constants in `CobblewardenConfig.java` to customize:

| Setting | Default | Description |
|---------|---------|-------------|
| `BOSS_HP` | 500 | The HP that the boss Pokemon should have (same as Warden) |
| `BOSS_LEVEL` | 100 | The level that the boss Pokemon spawns at |
| `ANCIENT_CITY_SPECIES` | "giratina_altered" | Species to spawn in Ancient Cities |
| `OVERWORLD_SPECIES` | "exploud" | Species to spawn outside Ancient Cities |
| `ANCIENT_CITY_REGION_SIZE` | 128 | Region size in blocks for spawn tracking |
| `PREVENT_NORMAL_BATTLE` | true | Whether to prevent normal Pokemon battles (Fight or Flight only) |
| `DEBUG_MODE` | false | Whether to show debug log messages |

### Available Species
- `"guzzlord"` - The Ultra Beast with massive bulk
- `"giratina_altered"` - The Renegade Pokemon in its Altered Forme
- `"exploud"` - The Loud Noise Pokemon

## Dependencies

### Required
- **Minecraft**: 1.21.1
- **Architectury API**: 13.0.8 or higher
- **Cobblemon**: 1.7.1-CF or higher
- **Fight or Flight Reborn**: 0.10.2 or higher

### Platform
- **Fabric**: Fabric Loader 0.18.4+, Fabric API 0.116.7+
- **NeoForge**: NeoForge 21.1.215+

## Installation

1. Download and install the required dependencies (Architectury API, Cobblemon, Fight or Flight Reborn)
2. Download the Cobblewarden Boss mod for your platform (Fabric or NeoForge)
3. Place the mod JAR file in your `mods` folder
4. Launch Minecraft

## How It Works

### Spawn Mechanics
1. When a Warden would normally spawn, the mod intercepts the spawn event
2. The mod checks the location to determine which species to spawn:
   - **Ancient City (deep_dark biome, Y < 0)**: Spawns configured ancient city species (default: Giratina)
   - **Overworld**: Spawns configured overworld species (default: Exploud)
3. The mod checks if this location has already spawned a boss (for Ancient Cities only)
4. If not already spawned:
   - The Warden spawn is cancelled
   - A boss Pokemon is spawned at the same location
   - For Ancient Cities, the location is marked as having spawned its boss
5. For Exploud in overworld, spawn cooldown works the same as vanilla Warden

### Battle System
- Boss Pokemon use Fight or Flight Reborn's battle system
- Players must engage it with their partner Pokemon
- The boss has high-level stats and is aggressive
- Cannot be caught with any Poké Ball (uncatchable property)
- **Normal Pokemon battles are prevented** - you cannot send out Pokemon to battle it traditionally
- Only Fight or Flight Reborn's partner combat system works

### Persistence
- Ancient City spawn tracking is stored in world save data
- Each Ancient City is identified by a region key based on coordinates
- Data persists across server restarts and world reloads
- Exploud spawns use vanilla Warden cooldown mechanics

## Technical Details

### Architecture
- **Common Module**: Contains all core logic (spawn interception, boss spawning, tracking)
- **Fabric Module**: Fabric-specific entry point and dependencies
- **NeoForge Module**: NeoForge-specific entry point and dependencies
- Uses Architectury's event system for cross-platform compatibility

### Key Components
- `WardenSpawnInterceptor`: Intercepts and cancels Warden spawns
- `BossSpawner`: Handles boss Pokemon creation and configuration with location detection
- `AncientCityTracker`: Manages persistent data for spawn tracking
- `BossDefeatHandler`: Handles death/defeat events
- `BossUtil`: Centralized NBT-based boss identification
- `LivingEntityMixin`: Prevents loot drops from boss Pokemon
- `PokemonEntityMixin`: Prevents normal Pokemon battles with boss Pokemon

## Building from Source

### Prerequisites
- JDK 21 or higher
- Gradle 8.5 or higher (or use the wrapper)

### Build Commands
```bash
# Build all platforms
./gradlew build

# Build Fabric only
./gradlew :fabric:build

# Build NeoForge only
./gradlew :neoforge:build
```

Build artifacts will be located in:
- Fabric: `fabric/build/libs/`
- NeoForge: `neoforge/build/libs/`

## Compatibility

### Minecraft Version
- 1.21.1

### Cobblemon Version
- 1.7.1-CF (Cobbleverse)

### Platform Support
- ✅ Fabric
- ✅ NeoForge

## Known Issues

- Gradle wrapper setup requires manual configuration due to repository access
- See BUILDING.md for build instructions

## License

See LICENSE file for details.

## Credits

- **Mod Author**: LiquidSilverAce
- **Cobblemon**: Cable, Hiroku, and the Cobblemon team
- **Fight or Flight Reborn**: LyquidQrystal
- **Architectury**: shedaniel and the Architectury team

## Support

For issues, suggestions, or questions, please visit the GitHub repository:
https://github.com/LiquidSilverAce/CobblewardenBoss
