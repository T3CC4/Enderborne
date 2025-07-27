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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndPortalBlock.class)
public class EndPortalAccessMixin {

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
            player.sendMessage(Text.literal("§5§oDefeat the End Dragon to break this barrier."));

            // Add visual/audio feedback
            spawnBlockedPortalEffects(world, pos, player);
        }
        // If dragon is defeated, allow normal teleportation
    }

    /**
     * Create visual and audio effects when portal access is blocked
     */
    private void spawnBlockedPortalEffects(World world, BlockPos pos, ServerPlayerEntity player) {
        if (world.isClient) return;

        // Spawn particles around the portal to indicate blocked access
        for (int i = 0; i < 12; i++) {
            double x = pos.getX() + 0.5 + (world.getRandom().nextDouble() - 0.5) * 2.0;
            double y = pos.getY() + 0.1;
            double z = pos.getZ() + 0.5 + (world.getRandom().nextDouble() - 0.5) * 2.0;

            // Spawn dark smoke particles to indicate blocked access
            world.addParticleClient(
                    net.minecraft.particle.ParticleTypes.LARGE_SMOKE,
                    x, y, z,
                    0, 0.05, 0
            );

            // Add some purple particles for the "corruption" theme
            if (world.getRandom().nextBoolean()) {
                world.addParticleClient(
                        net.minecraft.particle.ParticleTypes.PORTAL,
                        x, y, z,
                        (world.getRandom().nextDouble() - 0.5) * 0.5,
                        world.getRandom().nextDouble() * 0.2,
                        (world.getRandom().nextDouble() - 0.5) * 0.5
                );
            }
        }

        // Play a subtle rejection sound with some corruption atmosphere
        world.playSound(null, pos,
                net.minecraft.sound.SoundEvents.BLOCK_FIRE_EXTINGUISH,
                net.minecraft.sound.SoundCategory.BLOCKS,
                0.4f, 0.6f);

        // Add a low ominous sound
        world.playSound(null, pos,
                net.minecraft.sound.SoundEvents.BLOCK_PORTAL_AMBIENT,
                net.minecraft.sound.SoundCategory.BLOCKS,
                0.3f, 0.5f);
    }
}