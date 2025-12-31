package com.ace.cobbleboss.mixin;

import com.ace.cobbleboss.BossUtil;
import com.ace.cobbleboss.CobblewardenConfig;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to prevent normal Pokemon battles with boss Pokemon.
 * Players can only engage via Fight or Flight Reborn's partner combat.
 */
@Mixin(PokemonEntity.class)
public class PokemonEntityMixin {
    
    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void preventNormalBattle(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!CobblewardenConfig.PREVENT_NORMAL_BATTLE) {
            return;
        }
        
        PokemonEntity entity = (PokemonEntity) (Object) this;
        
        // Check if this is a boss Pokemon
        if (BossUtil.isBoss(entity)) {
            // Cancel the interaction (prevents battle initiation)
            // Fight or Flight Reborn will still allow partner combat via its own mechanics
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
