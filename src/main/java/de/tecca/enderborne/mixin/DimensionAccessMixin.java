package de.tecca.enderborne.mixin;

import de.tecca.enderborne.Enderborne;
import net.minecraft.block.BlockState;
import net.minecraft.block.EndPortalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndPortalBlock.class)
public class DimensionAccessMixin {

    /**
     * Control access through End Portals - block Overworld access until dragon is defeated
     */
    @Inject(method = "onEntityCollision", at = @At("HEAD"), cancellable = true)
    private void controlPortalAccess(BlockState state, World world, BlockPos pos, Entity entity, EntityCollisionHandler handler, CallbackInfo ci) {
        // Only control player access
        if (!(entity instanceof ServerPlayerEntity player)) {
            return;
        }

        // Only control access when going FROM End TO Overworld
        if (world.getRegistryKey() != World.END) {
            return; // Not in End, allow normal portal behavior
        }

        // Check if player has unlocked Overworld access
        if (!Enderborne.getDragonManager().hasUnlockedOverworld(player)) {
            // Block access to Overworld
            ci.cancel();

            // Send atmospheric message
            player.sendMessage(Text.literal("§8§oThe portal resists your passage..."), true);
            player.sendMessage(Text.literal("§7§oA powerful force blocks your path to the Overworld."));

            // Add visual/audio feedback
            spawnBlockedPortalEffects(world, pos, player);
        }
        // If dragon is defeated, allow normal teleportation
    }

    /**
     * Create visual and audio effects when portal access is blocked
     */
    @Unique
    private void spawnBlockedPortalEffects(World world, BlockPos pos, ServerPlayerEntity player) {
        if (world.isClient) return;

        // Spawn particles around the portal
        for (int i = 0; i < 8; i++) {
            double x = pos.getX() + 0.5 + (world.getRandom().nextDouble() - 0.5) * 1.5;
            double y = pos.getY() + 0.1;
            double z = pos.getZ() + 0.5 + (world.getRandom().nextDouble() - 0.5) * 1.5;

            // Spawn dark particles to indicate blocked access
            world.addParticleClient(
                    net.minecraft.particle.ParticleTypes.SMOKE,
                    x, y, z,
                    0, 0.05, 0
            );
        }

        // Play a subtle rejection sound
        world.playSound(null, pos,
                net.minecraft.sound.SoundEvents.BLOCK_FIRE_EXTINGUISH,
                net.minecraft.sound.SoundCategory.BLOCKS,
                0.3f, 0.5f);
    }
}