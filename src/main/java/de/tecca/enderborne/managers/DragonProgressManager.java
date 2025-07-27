package de.tecca.enderborne.managers;

import de.tecca.enderborne.Enderborne;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Manages dragon defeat progress and Overworld access
 * Uses modern Fabric Data Attachment API for persistent data
 */
public class DragonProgressManager {

    /**
     * Check if a player has defeated the Ender Dragon
     */
    public boolean hasDefeatedDragon(ServerPlayerEntity player) {
        return player.getAttached(Enderborne.DRAGON_DEFEATED);
    }

    /**
     * Check if a player has unlocked the Overworld
     */
    public boolean hasUnlockedOverworld(ServerPlayerEntity player) {
        return player.getAttached(Enderborne.OVERWORLD_UNLOCKED);
    }

    /**
     * Mark a player as having defeated the dragon and unlock Overworld access
     */
    public void markDragonDefeated(ServerPlayerEntity player) {
        // Record the achievement
        player.setAttached(Enderborne.DRAGON_DEFEATED, true);
        player.setAttached(Enderborne.OVERWORLD_UNLOCKED, true);
        player.setAttached(Enderborne.DEFEAT_TIMESTAMP, System.currentTimeMillis());

        Enderborne.LOGGER.info("Player {} has defeated the Ender Dragon and unlocked Overworld access",
                player.getName().getString());

        // Send achievement messages
        sendDragonDefeatMessages(player);

        // Create victory effects
        createVictoryEffects(player);
    }

    /**
     * Send messages when a player defeats the dragon
     */
    private void sendDragonDefeatMessages(ServerPlayerEntity player) {
        // Title message
        player.sendMessage(Text.literal("§6§l◆ THE END'S GUARDIAN HAS FALLEN ◆"), false);

        // Achievement messages
        player.sendMessage(Text.literal("§5§oThe ancient corruption weakens..."));
        player.sendMessage(Text.literal("§a§oYou feel the dimensional barriers shifting."));
        player.sendMessage(Text.literal("§e§lThe Overworld portal now responds to your presence!"));
        player.sendMessage(Text.literal("§7§oStep through the End Portal to claim your reward."));

        // Action bar message
        player.sendMessage(Text.literal("§6§l✦ OVERWORLD UNLOCKED ✦"), true);
    }

    /**
     * Create victory effects for dragon defeat
     */
    private void createVictoryEffects(ServerPlayerEntity player) {
        // Play victory sound
        player.playSoundToPlayer(
                net.minecraft.sound.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                net.minecraft.sound.SoundCategory.MASTER,
                1.0f, 1.0f
        );

        // Additional atmospheric sound with proper delay
        // Note: Using a simple delayed execution since we can't use Thread.sleep
        player.getWorld().getServer().execute(() -> {
            // Create a delayed task using server ticking
            new java.util.Timer().schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    if (!player.isRemoved()) { // Check if player is still valid
                        player.playSoundToPlayer(
                                net.minecraft.sound.SoundEvents.BLOCK_END_PORTAL_SPAWN,
                                net.minecraft.sound.SoundCategory.BLOCKS,
                                0.8f, 0.9f
                        );
                    }
                }
            }, 3000); // 3 seconds delay
        });
    }

    /**
     * Get the timestamp when the player defeated the dragon
     */
    public long getDefeatTimestamp(ServerPlayerEntity player) {
        return player.getAttached(Enderborne.DEFEAT_TIMESTAMP);
    }

    /**
     * Reset a player's dragon progress (for testing or admin commands)
     */
    public void resetProgress(ServerPlayerEntity player) {
        player.setAttached(Enderborne.DRAGON_DEFEATED, false);
        player.setAttached(Enderborne.OVERWORLD_UNLOCKED, false);
        player.setAttached(Enderborne.DEFEAT_TIMESTAMP, 0L);

        Enderborne.LOGGER.info("Reset dragon progress for player {}", player.getName().getString());
        player.sendMessage(Text.literal("§c§oYour progress has been reset. The dragon awaits..."));
    }

    /**
     * Check if enough time has passed since defeat for some time-based mechanics
     */
    public boolean hasTimePassed(ServerPlayerEntity player, long milliseconds) {
        if (!hasDefeatedDragon(player)) return false;

        long defeatTime = getDefeatTimestamp(player);
        return (System.currentTimeMillis() - defeatTime) >= milliseconds;
    }

    /**
     * Get a formatted string of when the dragon was defeated
     */
    public String getDefeatTimeFormatted(ServerPlayerEntity player) {
        long timestamp = getDefeatTimestamp(player);
        if (timestamp == 0) return "Never";

        long timePassed = System.currentTimeMillis() - timestamp;
        long minutes = timePassed / (60 * 1000);
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) return days + " day(s) ago";
        if (hours > 0) return hours + " hour(s) ago";
        if (minutes > 0) return minutes + " minute(s) ago";
        return "Just now";
    }
}