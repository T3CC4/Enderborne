package de.tecca.enderborne.managers;

import de.tecca.enderborne.Enderborne;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Manages player spawning and teleportation in the Enderborne mod
 * Uses modern Fabric Data Attachment API for persistent data
 */
public class PlayerSpawnManager {

    /**
     * Check if a player has played before in Enderborne
     */
    public boolean hasPlayedBefore(ServerPlayerEntity player) {
        return player.getAttached(Enderborne.HAS_PLAYED);
    }

    /**
     * Mark a player as having played before
     */
    public void markPlayerAsPlayed(ServerPlayerEntity player) {
        player.setAttached(Enderborne.HAS_PLAYED, true);

        // Track spawn count
        int currentCount = player.getAttached(Enderborne.SPAWN_COUNT);
        player.setAttached(Enderborne.SPAWN_COUNT, currentCount + 1);
    }

    /**
     * Get how many times a player has spawned
     */
    public int getSpawnCount(ServerPlayerEntity player) {
        return player.getAttached(Enderborne.SPAWN_COUNT);
    }

    /**
     * Teleport a player to the End islands (not the main dragon island)
     */
    public void teleportToEndIslands(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            Enderborne.LOGGER.error("Cannot teleport player {}: server is null", player.getName().getString());
            return;
        }

        ServerWorld endWorld = server.getWorld(World.END);
        if (endWorld == null) {
            Enderborne.LOGGER.error("Cannot teleport player {}: End dimension not available", player.getName().getString());
            return;
        }

        BlockPos spawnPos = findSafeEndSpawn(endWorld);

        try {
            // Use the correct teleport method for 1.21.8
            boolean success = player.teleport(endWorld,
                    spawnPos.getX() + 0.5,
                    spawnPos.getY(),
                    spawnPos.getZ() + 0.5,
                    java.util.Set.of(), // PositionFlag set - empty means no relative positioning
                    0.0f, 0.0f, // yaw, pitch
                    true); // yaw, pitch

            if (success) {
                Enderborne.LOGGER.info("Successfully teleported player {} to End islands at {}",
                        player.getName().getString(), spawnPos);
            } else {
                Enderborne.LOGGER.warn("Teleport returned false for player {} to End islands",
                        player.getName().getString());
            }

        } catch (Exception e) {
            Enderborne.LOGGER.error("Failed to teleport player {} to End islands: {}",
                    player.getName().getString(), e.getMessage());
        }
    }

    /**
     * Find a safe spawn location in the End, away from the main dragon island
     */
    private BlockPos findSafeEndSpawn(ServerWorld endWorld) {
        int attempts = 0;
        int maxAttempts = 50;

        while (attempts < maxAttempts) {
            // Generate coordinates away from main island (0,0)
            // End islands typically generate between 1000-2000 blocks from center
            int x = endWorld.getRandom().nextInt(1000) + 1000; // 1000-2000 blocks away
            int z = endWorld.getRandom().nextInt(1000) + 1000;

            // Randomly choose direction
            if (endWorld.getRandom().nextBoolean()) x = -x;
            if (endWorld.getRandom().nextBoolean()) z = -z;

            // Find a safe Y level
            BlockPos testPos = findGroundLevel(endWorld, new BlockPos(x, 100, z));

            if (testPos != null && isSafeSpawnLocation(endWorld, testPos)) {
                return testPos.up(); // Spawn one block above ground
            }

            attempts++;
        }

        // Fallback to a known safe location
        Enderborne.LOGGER.warn("Could not find safe End spawn after {} attempts, using fallback location", maxAttempts);
        return new BlockPos(1000, 50, 1000);
    }

    /**
     * Find the ground level at a given XZ coordinate
     */
    private BlockPos findGroundLevel(ServerWorld world, BlockPos startPos) {
        // Search downward from Y=100 to find solid ground
        for (int y = startPos.getY(); y > 0; y--) {
            BlockPos checkPos = new BlockPos(startPos.getX(), y, startPos.getZ());

            // Check if this is solid ground with air above
            if (!world.getBlockState(checkPos).isAir() &&
                    world.getBlockState(checkPos.up()).isAir() &&
                    world.getBlockState(checkPos.up(2)).isAir()) {
                return checkPos;
            }
        }
        return null;
    }

    /**
     * Check if a location is safe for spawning
     */
    private boolean isSafeSpawnLocation(ServerWorld world, BlockPos pos) {
        // Basic safety checks
        return !world.getBlockState(pos).isAir() && // Solid ground
                world.getBlockState(pos.up()).isAir() && // Clear head space
                world.getBlockState(pos.up(2)).isAir() && // Clear above head
                world.getFluidState(pos).isEmpty(); // Not in liquid
    }

    /**
     * Send welcome message to new players
     */
    public void sendWelcomeMessage(ServerPlayerEntity player) {
        player.sendMessage(Text.literal("§5§l◆ Welcome to the Enderborne Realm ◆"));
        player.sendMessage(Text.literal("§7§oYou find yourself in a corrupted dimension..."));
        player.sendMessage(Text.literal("§7§oThe Endermen here seem... different."));
        player.sendMessage(Text.literal("§6§oDefeat the End Dragon to unlock the path to the Overworld."));
    }

    /**
     * Send respawn message to players who died
     */
    public void sendRespawnMessage(ServerPlayerEntity player) {
        player.sendMessage(Text.literal("§8§oThe corruption pulls you back to the End..."));
        player.sendMessage(Text.literal("§7§oYou must grow stronger to escape this realm."));

        int spawnCount = getSpawnCount(player);
        if (spawnCount > 1) {
            player.sendMessage(Text.literal("§6§o(Death #" + (spawnCount - 1) + ")"));
        }
    }
}