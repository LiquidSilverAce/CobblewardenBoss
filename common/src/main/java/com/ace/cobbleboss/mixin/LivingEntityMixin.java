package com.ace.cobbleboss.mixin;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to prevent Guzzlord boss from dropping loot when defeated.
 */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    
    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
    private void preventGuzzlordLootDrop(DamageSource damageSource, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        
        // Check if this is a Guzzlord boss
        if (entity instanceof PokemonEntity pokemonEntity) {
            String customName = entity.getCustomName() != null ? entity.getCustomName().getString() : "";
            if (customName.contains("Guzzlord Boss")) {
                // Cancel the loot drop
                ci.cancel();
            }
        }
    }
}
