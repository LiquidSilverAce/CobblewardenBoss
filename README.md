# Cobblewarden Boss

A Minecraft mod for Cobbleverse 1.7.1-CF (Minecraft 1.21.1) that replaces the vanilla Warden with a powerful Guzzlord boss in Ancient Cities.

## Features

### Warden Replacement
- Intercepts vanilla Warden spawns in Ancient Cities
- Spawns a powerful Guzzlord boss instead
- Uses the same spawn triggers (sculk shrieker warnings, etc.)
- Maintains vanilla Warden spawn conditions

### Guzzlord Boss Configuration
- **Aggressive Behavior**: Guzzlord attacks players' Pokémon using Fight or Flight Reborn mechanics
- **Boosted Stats**: 
  - 500 HP (Warden-equivalent)
  - Level 75 with maximized IVs
  - Optimized EVs for HP and Attack
- **Uncatchable**: Cannot be captured with any Poké Ball
- **Boss Identity**: Custom name tag displaying "§4Guzzlord Boss§r"

### Defeat Behavior
- Guzzlord despawns upon defeat (similar to Warden)
- Does not drop any loot or items
- No death animation, simply disappears

### Per-Structure Cooldown
- Each Ancient City can only spawn one Guzzlord boss total
- Cooldown is persistent across:
  - Server restarts
  - Different players
  - World reloads
- Uses structure bounding box detection to identify unique Ancient Cities
- Data saved in world save files

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

## Configuration

Configuration values can be found in `CobblewardenConfig.java`:

| Setting | Default | Description |
|---------|---------|-------------|
| `GUZZLORD_HP` | 500 | The HP that the Guzzlord boss should have (same as Warden) |
| `GUZZLORD_LEVEL` | 75 | The level that the Guzzlord boss spawns at |
| `ANCIENT_CITY_REGION_SIZE` | 128 | Region size in blocks used to identify unique Ancient Cities |
| `DEBUG_MODE` | false | Whether to show debug log messages |

## How It Works

### Spawn Mechanics
1. When a Warden would normally spawn in an Ancient City, the mod intercepts the spawn event
2. The mod checks if this Ancient City has already spawned a Guzzlord using persistent saved data
3. If not already spawned:
   - The Warden spawn is cancelled
   - A Guzzlord is spawned at the same location
   - The Ancient City is marked as having spawned its boss
4. If already spawned, the Warden spawn is simply cancelled (no replacement)

### Battle System
- Guzzlord uses Fight or Flight Reborn's battle system
- Players must engage it with their Pokémon team
- The Guzzlord has high-level stats and is aggressive
- Cannot be caught with any Poké Ball (uncatchable property)

### Persistence
- Ancient City spawn tracking is stored in world save data
- Each Ancient City is identified by a region key based on coordinates
- Data persists across server restarts and world reloads

## Technical Details

### Architecture
- **Common Module**: Contains all core logic (spawn interception, Guzzlord spawning, tracking)
- **Fabric Module**: Fabric-specific entry point and dependencies
- **NeoForge Module**: NeoForge-specific entry point and dependencies
- Uses Architectury's event system for cross-platform compatibility

### Key Components
- `WardenSpawnInterceptor`: Intercepts and cancels Warden spawns
- `GuzzlordSpawner`: Handles Guzzlord creation and configuration
- `AncientCityTracker`: Manages persistent data for spawn tracking
- `GuzzlordDefeatHandler`: Handles death/defeat events
- `LivingEntityMixin`: Prevents loot drops from Guzzlord boss

## Building from Source

### Prerequisites
- JDK 21 or higher
- Gradle 8.10 or higher (or use the wrapper)

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

- None at this time

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
