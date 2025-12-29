# Building Cobblewarden Boss

This guide explains how to build the Cobblewarden Boss mod from source.

## Prerequisites

### Required Software
- **Java Development Kit (JDK)**: Version 21 or higher
  - Download from [Adoptium](https://adoptium.net/) or [Oracle](https://www.oracle.com/java/technologies/downloads/)
- **Gradle**: Version 8.5 or higher
  - Can be installed via package manager or downloaded from [Gradle.org](https://gradle.org/install/)

### IDE Support (Optional)
- **IntelliJ IDEA**: Recommended for Java development
- **Eclipse**: With Buildship Gradle plugin
- **Visual Studio Code**: With Java extensions

## Setup

### 1. Clone the Repository

```bash
git clone https://github.com/LiquidSilverAce/CobblewardenBoss.git
cd CobblewardenBoss
```

### 2. Setup Gradle Wrapper (if not already done)

The repository uses Architectury Loom which requires specific snapshot repositories. To set up the Gradle wrapper:

```bash
gradle wrapper --gradle-version 8.5
```

Note: If this fails due to snapshot repository issues, you can use your system Gradle installation directly.

## Building

### Using System Gradle

```bash
# Build all platforms
gradle build

# Build Fabric only
gradle :fabric:build

# Build NeoForge only
gradle :neoforge:build

# Clean build artifacts
gradle clean
```

### Using Gradle Wrapper (if set up successfully)

```bash
# Build all platforms
./gradlew build

# Build Fabric only
./gradlew :fabric:build

# Build NeoForge only
./gradlew :neoforge:build

# Clean build artifacts
./gradlew clean
```

## Build Artifacts

After a successful build, you'll find the compiled JAR files in:

- **Fabric**: `fabric/build/libs/cobblewarden_boss-fabric-<version>.jar`
- **NeoForge**: `neoforge/build/libs/cobblewarden_boss-neoforge-<version>.jar`

The files ending in `-dev-shadow.jar` are development builds and should not be used for distribution.

## Troubleshooting

### Gradle Wrapper Issues

If you encounter errors with the Gradle wrapper related to Architectury Loom SNAPSHOT versions:

1. Use your system Gradle installation instead
2. Ensure you have Gradle 8.5 or higher installed
3. Use the `gradle` command instead of `./gradlew`

### Dependency Resolution Failures

If dependencies fail to download:

1. Check your internet connection
2. Verify the repositories are accessible:
   - https://artefacts.cobblemon.com/
   - https://cursemaven.com
   - https://maven.architectury.dev/
3. Try clearing the Gradle cache:
   ```bash
   gradle clean --refresh-dependencies
   ```

### Compilation Errors

If you encounter compilation errors:

1. Ensure you're using JDK 21 or higher:
   ```bash
   java -version
   ```
2. Verify all dependencies are correctly configured in `build.gradle` files
3. Try rebuilding from a clean state:
   ```bash
   gradle clean build
   ```

### IDE Setup Issues

For IntelliJ IDEA:
1. Import the project as a Gradle project
2. Wait for Gradle sync to complete
3. If needed, refresh Gradle projects: View → Tool Windows → Gradle → Refresh

For Eclipse:
1. Import → Existing Gradle Project
2. Point to the repository root directory
3. Wait for workspace to build

## Development Builds

For development and testing:

```bash
# Run Fabric client
gradle :fabric:runClient

# Run NeoForge client
gradle :neoforge:runClient

# Run Fabric server
gradle :fabric:runServer

# Run NeoForge server
gradle :neoforge:runServer
```

## Publishing

The mod uses Maven publishing configuration. To publish to a local Maven repository:

```bash
gradle publishToMavenLocal
```

To publish to a remote Maven repository, configure the repository in the `publishing` block of `build.gradle`.

## Additional Notes

### Architectury Multi-Platform

This project uses Architectury to support both Fabric and NeoForge from a single codebase:

- **Common module**: Contains all shared code
- **Fabric module**: Fabric-specific implementations
- **NeoForge module**: NeoForge-specific implementations

When developing, make changes in the `common` module whenever possible to maintain cross-platform compatibility.

### Version Updates

To update the mod version:

1. Edit `gradle.properties`
2. Update the `mod_version` property
3. Rebuild the project

### Dependency Updates

To update dependencies (Cobblemon, Fight or Flight, etc.):

1. Check for new versions on CurseForge or Modrinth
2. Update the curse.maven artifact IDs in the respective `build.gradle` files
3. Update version requirements in `fabric.mod.json` and `neoforge.mods.toml`

## Support

For build-related issues, please check:
- [Architectury Documentation](https://docs.architectury.dev/)
- [Fabric Wiki](https://fabricmc.net/wiki/)
- [NeoForge Documentation](https://docs.neoforged.net/)

For mod-specific issues, visit the GitHub repository:
https://github.com/LiquidSilverAce/CobblewardenBoss/issues
