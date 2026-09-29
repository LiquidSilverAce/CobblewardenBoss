package com.ace.cobbleboss.mixin;

import com.ace.cobbleboss.combat.BossCombat;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PokemonEntity.class)
public abstract class BossPokemonMixin {
    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void cobblewarden$targetBeforeAi(CallbackInfo ci) {
        BossCombat.update((PokemonEntity) (Object) this);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void cobblewarden$updateBossCombat(CallbackInfo ci) {
        BossCombat.update((PokemonEntity) (Object) this);
    }
}
