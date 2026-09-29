package com.ace.cobbleboss.gametest;

import com.ace.cobbleboss.CobblewardenBoss;
import com.ace.cobbleboss.config.BossConfig;
import com.ace.cobbleboss.combat.BossCombat;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleStartedEvent;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
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
    public void triggeredSpawnsAndCityProgression(GameTestHelper helper) throws Exception {
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
            helper.assertTrue(!expectSpecies(helper, pos, "exploud").isUncatchable(), "Ordinary replacements remain catchable");
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
            ServerPlayer trigger = helper.makeMockServerPlayerInLevel();
            trigger.setGameMode(GameType.SURVIVAL);
            trigger.moveTo(pos.getX() + 12, pos.getY(), pos.getZ());
            trigger.getWardenSpawnTracker().orElseThrow().setWarningLevel(3);
            shrieker.tryShriek(level, trigger);
            CompoundTag savedShrieker = shrieker.saveWithoutMetadata(level.registryAccess());
            helper.assertTrue(savedShrieker.hasUUID("cobblewarden_boss_trigger"), "Accepted shriek must save its triggering player");
            shrieker.loadWithComponents(savedShrieker, level.registryAccess());
            shrieker.tryRespond(level);
            PokemonEntity giratina = expectSpecies(helper, pos, "giratina");
            helper.assertTrue(firstCity.equals(giratina.getPokemon().getPersistentData().getString(BossSpawner.CITY_KEY)),
                    "City identity must travel with the boss");
            helper.assertTrue(trigger.getUUID().equals(giratina.getPokemon().getPersistentData().getUUID(BossCombat.TRIGGER_KEY)),
                    "Boss must remember the player responsible for the warning");
            verifyCombat(helper, giratina, trigger);
            trigger.discard();
            CompoundTag savedBoss = new CompoundTag();
            helper.assertTrue(giratina.save(savedBoss), "Boss must be saveable");
            Entity reloaded = EntityType.loadEntityRecursive(savedBoss, level, entity -> entity);
            helper.assertTrue(reloaded instanceof PokemonEntity && reloaded.getTags().contains(BossSpawner.BOSS_TAG)
                            && firstCity.equals(((PokemonEntity) reloaded).getPokemon().getPersistentData().getString(BossSpawner.CITY_KEY)),
                    "Boss markers must survive chunk save/load");
            helper.assertTrue(((PokemonEntity) reloaded).isUncatchable()
                            && trigger.getUUID().equals(((PokemonEntity) reloaded).getPokemon().getPersistentData().getUUID(BossCombat.TRIGGER_KEY)),
                    "Capture protection and trigger identity must survive saving");
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
            helper.assertTrue(guzzlord.isUncatchable(), "Guzzlord must also default to uncatchable");
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

    private static void verifyCombat(GameTestHelper helper, PokemonEntity boss, ServerPlayer trigger) throws Exception {
        boolean reborn = Platform.isModLoaded("fightorflight");
        helper.assertTrue(boss.isUncatchable(), "Boss capture protection must default to on");
        if (!reborn) {
            boss.getEntityData().set(PokemonEntity.getUNBATTLEABLE(), true);
            boss.tick(); // Simulate loading a boss after removing Reborn.
        }
        helper.assertTrue(boss.getEntityData().get(PokemonEntity.getUNBATTLEABLE()) == reborn,
                "Open-world-only defaults on with Reborn, but must allow battles without it");
        ServerPlayer nearer = helper.makeMockServerPlayerInLevel();
        nearer.setGameMode(GameType.SURVIVAL);
        nearer.moveTo(boss.getX() + 3, boss.getY(), boss.getZ());
        trigger.moveTo(boss.getX() + 12, boss.getY(), boss.getZ());
        try {
            BossCombat.update(boss);
            helper.assertTrue(BossCombat.chooseTarget(boss) == trigger, "Trigger must take priority over a nearer player");
            if (reborn) {
                helper.assertTrue(boss.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) == trigger,
                        "Boss AI must target the triggering player");
                for (int tick = 0; tick < 5; tick++) boss.tick();
                helper.assertTrue(boss.getTarget() == trigger
                                && boss.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) == trigger,
                        "Normal entity AI ticks must preserve trigger priority");
                helper.assertTrue(!boss.canBattle(trigger), "Native battle challenge must be rejected");
                Class<?> utils = Class.forName("me.rufia.fightorflight.utils.PokemonUtils");
                Object rebornConfig = Class.forName("me.rufia.fightorflight.CobblemonFightOrFlight")
                        .getMethod("commonConfig").invoke(null);
                var minimumLevel = rebornConfig.getClass().getField("minimum_attack_level");
                var forceBattle = rebornConfig.getClass().getField("force_wild_battle_on_player_hurt");
                Object oldMinimum = minimumLevel.get(rebornConfig), oldForceBattle = forceBattle.get(rebornConfig);
                try {
                    minimumLevel.setInt(rebornConfig, 101);
                    forceBattle.setBoolean(rebornConfig, true);
                    helper.assertTrue((boolean) utils.getMethod("shouldFightTarget", PokemonEntity.class).invoke(null, boss),
                            "Boss aggression must override Reborn's ordinary attack-level restriction");
                    helper.assertTrue(!(boolean) utils.getMethod("shouldAvoid", PokemonEntity.class).invoke(null, boss),
                            "Boss must not flee");
                    helper.assertTrue(!(boolean) utils.getMethod("pokemonTryForceEncounter", PokemonEntity.class, Entity.class)
                            .invoke(null, boss, trigger), "Even enabled forced encounters must not replace boss world attacks");
                } finally {
                    minimumLevel.set(rebornConfig, oldMinimum);
                    forceBattle.set(rebornConfig, oldForceBattle);
                }
                helper.assertTrue(!(boolean) utils.getMethod("pokemonForceEncounterPvE", ServerPlayer.class, PokemonEntity.class)
                        .invoke(null, trigger, boss), "Reborn must not send a forced battle prompt");
                // Expire vanilla login protection before testing real damage.
                for (int tick = 0; tick < 61; tick++) trigger.tick();
                float health = trigger.getHealth();
                Class<?> attacks = Class.forName("me.rufia.fightorflight.entity.PokemonAttackEffect");
                attacks.getMethod("pokemonAttack", PokemonEntity.class, Entity.class).invoke(null, boss, trigger);
                helper.assertTrue(trigger.getHealth() < health, "Reborn boss attacks must actually damage players");
                trigger.setHealth(trigger.getMaxHealth());
                PokemonProperties petProperties = new PokemonProperties();
                petProperties.setSpecies("tyranitar");
                PokemonEntity pet = petProperties.createEntity(helper.getLevel());
                PlayerPartyStore party = new PlayerPartyStore(trigger.getUUID());
                helper.assertTrue(party.add(pet.getPokemon()), "Could not prepare player-owned combat Pokémon");
                pet.getPokemon().setState(new SentOutState(pet));
                float bossHealth = boss.getHealth();
                boss.hurt(helper.getLevel().damageSources().mobAttack(pet), 1);
                helper.assertTrue(boss.getHealth() < bossHealth, "Battle restriction must not block open-world Pokémon damage");
                pet.discard();
            }
            var actor = new PlayerBattleActor(trigger.getUUID(), List.of(BattlePokemon.Companion.playerOwned(boss.getPokemon())));
            var opponent = new PlayerBattleActor(nearer.getUUID(), List.of());
            var battle = new PokemonBattle(new BattleFormat(), new BattleSide(actor), new BattleSide(opponent));
            var event = new BattleStartedEvent.Pre(battle, null);
            CobblemonEvents.BATTLE_STARTED_PRE.post(event);
            helper.assertTrue(event.isCanceled() == reborn, "Programmatic battles must respect the combat restriction");
            trigger.setGameMode(GameType.CREATIVE);
            BossCombat.update(boss);
            helper.assertTrue(BossCombat.chooseTarget(boss) == nearer, "Unavailable trigger must fall back to the nearest player");
            trigger.setGameMode(GameType.SURVIVAL);
            trigger.moveTo(boss.getX() + 100, boss.getY(), boss.getZ());
            helper.assertTrue(BossCombat.chooseTarget(boss) == nearer, "Out-of-range trigger must use the nearest player");
            configure(new BossConfig(true, "dusknoir", "giratina", 70, false, false, false));
            BossCombat.update(boss);
            helper.assertTrue(!boss.isUncatchable() && !boss.getEntityData().get(PokemonEntity.getUNBATTLEABLE()),
                    "Capture and regular battles must be independently configurable");
            if (reborn) helper.assertTrue(boss.getTarget() == null, "Disabling forced aggression must release its target");
            configure(new BossConfig(true, "dusknoir", "giratina", 70));
            BossCombat.update(boss);
        } finally {
            nearer.discard();
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
