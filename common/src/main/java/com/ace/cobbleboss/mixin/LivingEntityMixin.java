package com.ace.cobbleboss.mixin;

import com.ace.cobbleboss.BossUtil;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to prevent boss Pokemon from dropping loot when defeated.
 */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    
    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
    private void preventBossLootDrop(DamageSource damageSource, LivingEntity attacker, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        
        // Check if this is a boss Pokemon using utility method
        if (BossUtil.isBoss(entity)) {
            // Cancel the loot drop
            ci.cancel();
        }
    }
}
