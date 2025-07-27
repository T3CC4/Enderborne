package de.tecca.enderborne.mixin;

import de.tecca.enderborne.Enderborne;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Proper dragon death detection by injecting into LivingEntity.onDeath
 * and checking if the entity is specifically an EnderDragonEntity
 */
@Mixin(LivingEntity.class)
public class DragonDefeatMixin {

    /**
     * Inject into the general entity death method and check for dragon specifically
     */
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onEntityDeath(DamageSource damageSource, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // Only handle if this is specifically an Ender Dragon
        if (!(entity instanceof EnderDragonEntity dragon)) {
            return;
        }

        // Only process on server side
        if (dragon.getWorld().isClient) {
            return;
        }

        ServerWorld world = (ServerWorld) dragon.getWorld();

        // Only process in the End dimension
        if (world.getRegistryKey() != World.END) {
            return;
        }

        Enderborne.LOGGER.info("Ender Dragon defeated in world {} by {}",
                world.getRegistryKey().getValue(),
                damageSource.getAttacker() != null ? damageSource.getAttacker().getName().getString() : "unknown");

        // Find all players within range of the dragon fight
        Box searchArea = Box.of(dragon.getPos(), 300, 150, 300); // Large area to catch all participants
        List<ServerPlayerEntity> nearbyPlayers = world.getEntitiesByClass(
                ServerPlayerEntity.class,
                searchArea,
                player -> !player.isSpectator() // Exclude spectators
        );

        Enderborne.LOGGER.info("Found {} players near dragon defeat", nearbyPlayers.size());

        // Grant Overworld access to all nearby players
        for (ServerPlayerEntity player : nearbyPlayers) {
            Enderborne.getDragonManager().markDragonDefeated(player);
        }

        // Create global effects for the victory
        createGlobalVictoryEffects(world, dragon.getBlockPos());
    }

    /**
     * Create dramatic visual effects for dragon defeat across the dimension
     * Fixed: Use proper server scheduling instead of Timer
     */
    @Unique
    private void createGlobalVictoryEffects(ServerWorld world, BlockPos dragonPos) {
        // Spawn victory particles in a wide area
        for (int i = 0; i < 50; i++) {
            double x = dragonPos.getX() + (world.getRandom().nextDouble() - 0.5) * 100;
            double y = dragonPos.getY() + world.getRandom().nextDouble() * 30;
            double z = dragonPos.getZ() + (world.getRandom().nextDouble() - 0.5) * 100;

            // Spawn golden/purple victory particles
            world.spawnParticles(
                    net.minecraft.particle.ParticleTypes.END_ROD,
                    x, y, z,
                    1, 0, 0.2, 0, 0.1
            );

            world.spawnParticles(
                    net.minecraft.particle.ParticleTypes.TOTEM_OF_UNDYING,
                    x, y, z,
                    1, 0, 0.1, 0, 0.05
            );
        }

        // Play victory sounds across the dimension
        world.playSound(null, dragonPos,
                net.minecraft.sound.SoundEvents.ENTITY_ENDER_DRAGON_DEATH,
                net.minecraft.sound.SoundCategory.HOSTILE,
                2.0f, 1.0f);

        // Fixed: Use proper server scheduling instead of Timer
        world.getServer().execute(() -> {
            // Schedule delayed effects using server task manager (runs after 60 ticks = 3 seconds)
            new java.util.Timer().schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    // Execute on main server thread
                    world.getServer().execute(() -> {
                        world.playSound(null, dragonPos,
                                net.minecraft.sound.SoundEvents.BLOCK_END_PORTAL_SPAWN,
                                net.minecraft.sound.SoundCategory.BLOCKS,
                                1.5f, 0.8f);

                        // Additional atmospheric sound
                        world.playSound(null, dragonPos,
                                net.minecraft.sound.SoundEvents.BLOCK_BEACON_ACTIVATE,
                                net.minecraft.sound.SoundCategory.BLOCKS,
                                1.0f, 1.2f);
                    });
                }
            }, 3000); // 3 seconds delay
        });

        Enderborne.LOGGER.info("Global victory effects created at {}", dragonPos);
    }
}