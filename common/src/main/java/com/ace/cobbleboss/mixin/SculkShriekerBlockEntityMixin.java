package com.ace.cobbleboss.mixin;

import com.ace.cobbleboss.CobblewardenBoss;
import com.ace.cobbleboss.spawn.BossSpawner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.SculkShriekerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SculkShriekerBlockEntity.class)
public abstract class SculkShriekerBlockEntityMixin {
    @Shadow private int warningLevel;

    @Inject(method = "trySummonWarden", at = @At("HEAD"), cancellable = true)
    private void cobblewarden$replaceTriggeredWarden(ServerLevel level, CallbackInfoReturnable<Boolean> cir) {
        if (CobblewardenBoss.config().enabled()) {
            // tryRespond has already checked can_summon, difficulty, and doWardenSpawning.
            // Always cancel vanilla while enabled, including when no safe Pokémon position exists.
            cir.setReturnValue(warningLevel >= 4 && BossSpawner.trySpawn(level,
                    ((SculkShriekerBlockEntity) (Object) this).getBlockPos()));
        }
    }
}
