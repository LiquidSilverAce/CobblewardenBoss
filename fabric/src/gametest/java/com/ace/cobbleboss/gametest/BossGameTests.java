package com.ace.cobbleboss.gametest;

import com.ace.cobbleboss.CobblewardenBoss;
import com.ace.cobbleboss.config.BossConfig;
import com.ace.cobbleboss.spawn.BossSpawner;
import com.ace.cobbleboss.state.DefeatedCities;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.activestate.SentOutState;
import com.google.gson.Gson;
import dev.architectury.platform.Platform;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.warden.WardenSpawnTracker;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.entity.SculkShriekerBlockEntity;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.phys.AABB;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

/** Exercises the real mixins and Cobblemon entities in a dedicated server. */
public final class BossGameTests implements FabricGameTest {
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void triggeredSpawnsAndCityProgression(GameTestHelper helper) throws IOException {
        ServerLevel level = helper.getLevel();
        level.getServer().overworld().getDataStorage().set(CobblewardenBoss.MOD_ID + "_cities", new DefeatedCities());
        BlockPos pos = helper.absolutePos(new BlockPos(24, 3, 24));
        level.getServer().setDifficulty(Difficulty.NORMAL, true);
        level.getGameRules().getRule(GameRules.RULE_DO_WARDEN_SPAWNING).set(true, level.getServer());
        // Cobbleverse disables ordinary hostile spawning; the shrieker uses its own gamerule.
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
        try {
            configure(BossConfig.DEFAULT);
            arena(level, pos);
            SculkShriekerBlockEntity shrieker = shrieker(level, pos, true);
            helper.assertTrue(BossSpawner.ancientCityAt(level, pos) == null, "Ordinary terrain must not count as a city");
            for (int warning = 0; warning < 4; warning++) {
                respond(level, shrieker, warning);
                helper.assertTrue(pokemon(level, pos).isEmpty(), "Spawned before the fourth warning");
            }
            respond(level, shrieker, 4);
            expectSpecies(helper, pos, "exploud");
            helper.assertTrue(wardens(level, pos).isEmpty(), "A vanilla Warden leaked through");
            helper.assertTrue(WardenSpawnTracker.tryWarn(level, pos, helper.makeMockServerPlayerInLevel()).isEmpty(),
                    "Replacement must suppress further warnings like a nearby Warden");
            respond(level, shrieker, 4);
            helper.assertTrue(pokemon(level, pos).size() == 1, "Simultaneous shriekers must not duplicate the boss");
            clear(level, pos);

            configure(new BossConfig(true, "dusknoir", "giratina", 70));
            respond(level, shrieker, 4);
            expectSpecies(helper, pos, "dusknoir");
            clear(level, pos);

            level.getGameRules().getRule(GameRules.RULE_DO_WARDEN_SPAWNING).set(false, level.getServer());
            respond(level, shrieker, 4);
            helper.assertTrue(pokemon(level, pos).isEmpty(), "doWardenSpawning=false must suppress replacements");
            level.getGameRules().getRule(GameRules.RULE_DO_WARDEN_SPAWNING).set(true, level.getServer());
            level.getServer().setDifficulty(Difficulty.PEACEFUL, true);
            respond(level, shrieker, 4);
            helper.assertTrue(pokemon(level, pos).isEmpty(), "Peaceful must suppress replacements");
            level.getServer().setDifficulty(Difficulty.NORMAL, true);
            respond(level, shrieker(level, pos, false), 4);
            helper.assertTrue(pokemon(level, pos).isEmpty(), "Player-placed shriekers must not summon");
            shrieker = shrieker(level, pos, true);

            // A sealed chamber cannot fit Dusknoir. Failure must not fall back to a Warden.
            for (BlockPos block : BlockPos.betweenClosed(pos.offset(-7, 1, -7), pos.offset(7, 7, 7))) {
                level.setBlockAndUpdate(block, Blocks.DEEPSLATE.defaultBlockState());
            }
            respond(level, shrieker, 4);
            helper.assertTrue(pokemon(level, pos).isEmpty() && wardens(level, pos).isEmpty(),
                    "Blocked spawn space must produce neither a Pokémon nor a Warden");
            arena(level, pos);
            shrieker = shrieker(level, pos, true);

            configure(new BossConfig(false, "exploud", "default", 70));
            respond(level, shrieker, 4);
            boolean wardensBlacklisted = Platform.isModLoaded("mobsbegone");
            helper.assertTrue(wardens(level, pos).size() == (wardensBlacklisted ? 0 : 1),
                    "Disabling replacements must restore vanilla spawning, including other mods' restrictions");
            helper.assertTrue(pokemon(level, pos).isEmpty(), "Disabled replacements must not produce Pokémon");
            clear(level, pos);
            configure(new BossConfig(true, "dusknoir", "giratina", 70));
            Warden summoned = EntityType.WARDEN.create(level);
            helper.assertTrue(summoned != null, "Could not create command-style Warden");
            summoned.moveTo(pos.getX(), pos.getY(), pos.getZ());
            level.addFreshEntity(summoned);
            respond(level, shrieker, 4);
            if (wardensBlacklisted) {
                helper.assertTrue(wardens(level, pos).isEmpty(), "The pack's Warden blacklist must still apply");
                expectSpecies(helper, pos, "dusknoir");
            } else {
                helper.assertTrue(wardens(level, pos).size() == 1 && pokemon(level, pos).isEmpty(),
                        "Existing Wardens must remain and prevent replacements");
            }
            clear(level, pos);

            installCity(level, pos);
            String firstCity = BossSpawner.ancientCityAt(level, pos);
            helper.assertTrue(firstCity != null, "Actual structure bounds must identify an Ancient City");
            respond(level, shrieker, 4);
            PokemonEntity giratina = expectSpecies(helper, pos, "giratina");
            helper.assertTrue(firstCity.equals(giratina.getPokemon().getPersistentData().getString(BossSpawner.CITY_KEY)),
                    "City identity must travel with the boss");
            CompoundTag savedBoss = new CompoundTag();
            helper.assertTrue(giratina.save(savedBoss), "Boss must be saveable");
            Entity reloaded = EntityType.loadEntityRecursive(savedBoss, level, entity -> entity);
            helper.assertTrue(reloaded instanceof PokemonEntity && reloaded.getTags().contains(BossSpawner.BOSS_TAG)
                            && firstCity.equals(((PokemonEntity) reloaded).getPokemon().getPersistentData().getString(BossSpawner.CITY_KEY)),
                    "Boss markers must survive chunk save/load");
            reloaded.discard();
            DefeatedCities cities = DefeatedCities.get(level.getServer());
            helper.assertTrue(!cities.isDefeated(firstCity), "Spawning alone must not clear the city");
            // This is the same HP update used by Cobblemon's FaintInstruction in battles.
            giratina.getPokemon().setCurrentHealth(0);
            helper.assertTrue(cities.isDefeated(firstCity), "A boss faint must clear its city");
            clear(level, pos);
            respond(level, shrieker, 4);
            expectSpecies(helper, pos, "dusknoir");
            clear(level, pos);

            DefeatedCities restored = DefeatedCities.load(cities.save(new CompoundTag(), level.registryAccess()), level.registryAccess());
            helper.assertTrue(restored.isDefeated(firstCity), "City completion must survive serialization");
            helper.assertTrue(!restored.isDefeated("minecraft:the_nether/" + new ChunkPos(pos).x + "/" + new ChunkPos(pos).z),
                    "Equal chunk coordinates in different dimensions must be independent");

            BlockPos other = pos.offset(128, 0, 0);
            arena(level, other);
            installCity(level, other);
            configure(new BossConfig(true, "exploud", "guzzlord", 70));
            SculkShriekerBlockEntity secondShrieker = shrieker(level, other, true);
            respond(level, secondShrieker, 4);
            PokemonEntity guzzlord = expectSpecies(helper, other, "guzzlord");
            String secondCity = BossSpawner.ancientCityAt(level, other);
            helper.assertTrue(!firstCity.equals(secondCity) && !cities.isDefeated(secondCity),
                    "Defeating one city must leave another city's boss available");
            guzzlord.hurt(level.damageSources().genericKill(), Float.MAX_VALUE);
            helper.assertTrue(cities.isDefeated(secondCity), "Defeating a boss in the overworld must also count");
            clear(level, other);
            respond(level, secondShrieker, 4);
            expectSpecies(helper, other, "exploud");
            clear(level, other);

            // Ordinary Pokémon of a boss species cannot clear an unvisited city.
            PokemonProperties properties = new PokemonProperties();
            properties.setSpecies("giratina");
            PokemonEntity ordinary = properties.createEntity(level);
            ordinary.getPokemon().getPersistentData().putString(BossSpawner.CITY_KEY, "unvisited");
            ordinary.getPokemon().setState(new SentOutState(ordinary));
            ordinary.getPokemon().setCurrentHealth(0);
            helper.assertTrue(!cities.isDefeated("unvisited"), "Unmarked Pokémon cannot affect city progression");
            ordinary.discard();

            PokemonEntity captured = properties.createEntity(level);
            captured.addTag(BossSpawner.BOSS_TAG);
            captured.getPokemon().getPersistentData().putString(BossSpawner.CITY_KEY, "captured-city");
            PlayerPartyStore party = new PlayerPartyStore(UUID.randomUUID());
            helper.assertTrue(party.add(captured.getPokemon()), "Could not put captured boss in a party");
            captured.getPokemon().setState(new SentOutState(captured));
            captured.getPokemon().setCurrentHealth(0);
            helper.assertTrue(!cities.isDefeated("captured-city"), "A captured boss fainting must not clear a city");
            captured.discard();
            helper.succeed();
        } finally {
            clear(level, pos);
            configure(BossConfig.DEFAULT);
        }
    }

    private static void configure(BossConfig config) throws IOException {
        var path = Platform.getConfigFolder().resolve(CobblewardenBoss.MOD_ID + ".json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, new Gson().toJson(config));
        CobblewardenBoss.reloadConfig();
    }

    private static void arena(ServerLevel level, BlockPos center) {
        for (BlockPos block : BlockPos.betweenClosed(center.offset(-8, -1, -8), center.offset(8, 8, 8))) {
            level.setBlockAndUpdate(block, (block.getY() == center.getY() - 1 ? Blocks.DEEPSLATE : Blocks.AIR).defaultBlockState());
        }
    }

    private static SculkShriekerBlockEntity shrieker(ServerLevel level, BlockPos pos, boolean canSummon) {
        level.setBlockAndUpdate(pos, Blocks.SCULK_SHRIEKER.defaultBlockState().setValue(SculkShriekerBlock.CAN_SUMMON, canSummon));
        return (SculkShriekerBlockEntity) level.getBlockEntity(pos);
    }

    private static void respond(ServerLevel level, SculkShriekerBlockEntity shrieker, int warning) {
        CompoundTag data = new CompoundTag();
        data.putInt("warning_level", warning);
        shrieker.loadWithComponents(data, level.registryAccess());
        shrieker.tryRespond(level);
    }

    private static List<PokemonEntity> pokemon(ServerLevel level, BlockPos pos) {
        return level.getEntitiesOfClass(PokemonEntity.class, new AABB(pos).inflate(20));
    }

    private static List<Warden> wardens(ServerLevel level, BlockPos pos) {
        return level.getEntitiesOfClass(Warden.class, new AABB(pos).inflate(20));
    }

    private static PokemonEntity expectSpecies(GameTestHelper helper, BlockPos pos, String species) {
        List<PokemonEntity> spawned = pokemon(helper.getLevel(), pos);
        helper.assertTrue(spawned.size() == 1, "Expected one " + species + ", found " + spawned.size());
        PokemonEntity entity = spawned.getFirst();
        helper.assertTrue(entity.getPokemon().getSpecies().getResourceIdentifier().getPath().equals(species),
                "Expected " + species + ", got " + entity.getPokemon().getSpecies().getName());
        helper.assertTrue(entity.getPokemon().getLevel() == 70, "Configured level was not applied");
        helper.assertTrue(helper.getLevel().noCollision(entity), "Spawned Pokémon intersects a block or entity");
        return entity;
    }

    private static void clear(ServerLevel level, BlockPos pos) {
        pokemon(level, pos).forEach(Entity::discard);
        wardens(level, pos).forEach(Entity::discard);
    }

    private static void installCity(ServerLevel level, BlockPos pos) {
        Structure city = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(BuiltinStructures.ANCIENT_CITY);
        ChunkPos chunk = new ChunkPos(pos);
        StructurePiece piece = new StructurePiece(StructurePieceType.NETHER_FOSSIL, 0,
                new BoundingBox(pos.getX() - 8, pos.getY() - 4, pos.getZ() - 8,
                        pos.getX() + 8, pos.getY() + 8, pos.getZ() + 8)) {
            @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {}
            @Override public void postProcess(WorldGenLevel world, StructureManager manager, ChunkGenerator generator,
                                              RandomSource random, BoundingBox bounds, ChunkPos chunkPos, BlockPos origin) {}
        };
        StructureStart start = new StructureStart(city, chunk, 0, new PiecesContainer(List.of(piece)));
        level.getChunk(chunk.x, chunk.z).setStartForStructure(city, start);
        level.getChunkAt(pos).addReferenceForStructure(city, chunk.toLong());
    }
}
