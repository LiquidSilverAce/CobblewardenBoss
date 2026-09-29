package com.ace.cobbleboss.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.SpawnUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SpawnUtil.class)
public interface SpawnUtilAccessor {
    @Invoker("moveToPossibleSpawnPosition")
    static boolean cobblewarden$moveToPossibleSpawnPosition(ServerLevel level, int range,
                                                          BlockPos.MutableBlockPos pos, SpawnUtil.Strategy strategy) {
        throw new AssertionError("Mixin invoker was not applied");
    }
}
