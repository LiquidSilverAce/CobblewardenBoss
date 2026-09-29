package com.ace.cobbleboss.mixin;

import com.ace.cobbleboss.CobblewardenBoss;
import com.ace.cobbleboss.spawn.BossSpawner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.warden.WardenSpawnTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WardenSpawnTracker.class)
public abstract class WardenSpawnTrackerMixin {
    @Inject(method = "hasNearbyWarden", at = @At("RETURN"), cancellable = true)
    private static void cobblewarden$countNearbyReplacements(ServerLevel level, BlockPos pos,
                                                            CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && CobblewardenBoss.config().enabled()
                && BossSpawner.hasNearbyReplacement(level, pos)) {
            cir.setReturnValue(true);
        }
    }
}
