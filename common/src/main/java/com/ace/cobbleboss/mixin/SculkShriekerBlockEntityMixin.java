package com.ace.cobbleboss.mixin;

import com.ace.cobbleboss.CobblewardenBoss;
import com.ace.cobbleboss.spawn.BossSpawner;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.SculkShriekerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(SculkShriekerBlockEntity.class)
public abstract class SculkShriekerBlockEntityMixin {
    @Shadow private int warningLevel;
    @Unique private UUID cobblewarden$trigger;

    @Inject(method = "shriek", at = @At("HEAD"))
    private void cobblewarden$rememberTrigger(ServerLevel level, Entity source, CallbackInfo ci) {
        cobblewarden$trigger = source instanceof ServerPlayer player ? player.getUUID() : null;
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void cobblewarden$saveTrigger(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        if (cobblewarden$trigger != null) tag.putUUID("cobblewarden_boss_trigger", cobblewarden$trigger);
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void cobblewarden$loadTrigger(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        cobblewarden$trigger = tag.hasUUID("cobblewarden_boss_trigger") ? tag.getUUID("cobblewarden_boss_trigger") : null;
    }

    @Inject(method = "tryRespond", at = @At("RETURN"))
    private void cobblewarden$clearTrigger(ServerLevel level, CallbackInfo ci) {
        cobblewarden$trigger = null;
    }

    @Inject(method = "trySummonWarden", at = @At("HEAD"), cancellable = true)
    private void cobblewarden$replaceTriggeredWarden(ServerLevel level, CallbackInfoReturnable<Boolean> cir) {
        if (CobblewardenBoss.config().enabled()) {
            // tryRespond has already checked can_summon, difficulty, and doWardenSpawning.
            // Always cancel vanilla while enabled, including when no safe Pokémon position exists.
            cir.setReturnValue(warningLevel >= 4 && BossSpawner.trySpawn(level,
                    ((SculkShriekerBlockEntity) (Object) this).getBlockPos(), cobblewarden$trigger));
        }
    }
}
