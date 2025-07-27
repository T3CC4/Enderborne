package de.tecca.enderborne.mixin;

import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EndermanEntity.class)
public class EndermanPeacefulMixin {

    /**
     * Prevents Endermen from getting angry when players look at their eyes
     */
    @Inject(method = "isPlayerStaring", at = @At("HEAD"), cancellable = true)
    private void preventAngerFromStaring(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        // Always return false - Endermen never consider themselves being stared at
        cir.setReturnValue(false);
    }

    /**
     * Prevents Endermen from becoming angry at players
     */
    @Inject(method = "setAngryAt", at = @At("HEAD"), cancellable = true)
    private void preventTargetingPlayers(java.util.UUID angryAt, CallbackInfo ci) {
        // Cancel setting anger target - Endermen remain peaceful toward players
        ci.cancel();
    }
}