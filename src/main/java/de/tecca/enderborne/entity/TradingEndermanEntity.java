package de.tecca.enderborne.entity;

import de.tecca.enderborne.Enderborne;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

/**
 * A simplified peaceful Enderman that can trade with players
 * Spawns rarely in the End and offers unique End-related trades
 * Properly registered as a custom entity in Fabric 1.21.8
 */
public class TradingEndermanEntity extends EndermanEntity {

    private int restockTimer = 0;
    private boolean tradingAvailable = true;
    private static final int RESTOCK_TIME = 2400; // 2 minutes

    // Simple trade map to avoid complex TradeOffer API
    private static final Map<ItemStack, ItemStack> TRADES = new HashMap<>();

    static {
        // Initialize trades using simple ItemStack pairs
        TRADES.put(new ItemStack(Items.ENDER_PEARL, 3), new ItemStack(Items.CHORUS_FRUIT, 5));
        TRADES.put(new ItemStack(Items.CHORUS_FRUIT, 8), new ItemStack(Items.CHORUS_FLOWER, 1));
        TRADES.put(new ItemStack(Items.END_STONE, 32), new ItemStack(Items.PURPUR_BLOCK, 8));
        TRADES.put(new ItemStack(Items.PURPUR_BLOCK, 16), new ItemStack(Items.PURPUR_PILLAR, 4));
        TRADES.put(new ItemStack(Items.SHULKER_SHELL, 1), new ItemStack(Items.ENDER_CHEST, 1));
        TRADES.put(new ItemStack(Items.ENDER_CHEST, 1), new ItemStack(Items.ENDER_EYE, 2));
        TRADES.put(new ItemStack(Items.DRAGON_BREATH, 1), new ItemStack(Items.EXPERIENCE_BOTTLE, 8));
        TRADES.put(new ItemStack(Items.SCULK, 16), new ItemStack(Items.SCULK_CATALYST, 1));
        TRADES.put(new ItemStack(Items.SCULK_SENSOR, 4), new ItemStack(Items.SCULK_SHRIEKER, 1));
    }

    // Constructor for our custom entity type
    public TradingEndermanEntity(EntityType<? extends EndermanEntity> entityType, World world) {
        super(entityType, world);
        this.setCanPickUpLoot(false);
    }

    /**
     * REQUIRED: Create default attributes for this entity type
     * This method is called during entity registration
     */
    public static DefaultAttributeContainer.Builder createTradingEndermanAttributes() {
        return EndermanEntity.createEndermanAttributes()
                .add(EntityAttributes.MAX_HEALTH, 40.0) // Same as Enderman
                .add(EntityAttributes.MOVEMENT_SPEED, 0.3) // Same as Enderman
                .add(EntityAttributes.ATTACK_DAMAGE, 7.0) // Same as Enderman (but won't attack)
                .add(EntityAttributes.FOLLOW_RANGE, 64.0); // Same as Enderman
    }

    @Override
    protected void initGoals() {
        // Clear existing goals with proper predicate
        this.goalSelector.clear(goal -> true);
        this.targetSelector.clear(goal -> true);

        // Add only peaceful goals
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new EscapeDangerGoal(this, 2.0));
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(4, new LookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.getWorld().isClient) {
            // Spawn unique particles around the trading enderman
            spawnTradingParticles();

            // Handle restocking
            if (restockTimer > 0) {
                restockTimer--;
                if (restockTimer <= 0) {
                    restockTrades();
                }
            }
        }
    }

    /**
     * Spawn unique particles to distinguish trading endermen
     */
    private void spawnTradingParticles() {
        if (this.getWorld().isClient) return;

        Random random = this.getRandom();
        ServerWorld world = (ServerWorld) this.getWorld();

        // Spawn fewer particles than normal to not be overwhelming
        if (random.nextInt(10) == 0) { // 10% chance each tick
            // Spawn mystical particles around the enderman
            double x = this.getX() + (random.nextDouble() - 0.5) * 2.0;
            double y = this.getY() + random.nextDouble() * 3.0;
            double z = this.getZ() + (random.nextDouble() - 0.5) * 2.0;

            // Use enchanting table particles for a mystical trading effect
            world.spawnParticles(
                    ParticleTypes.ENCHANT,
                    x, y, z,
                    1,
                    (random.nextDouble() - 0.5) * 0.2,
                    random.nextDouble() * 0.2,
                    (random.nextDouble() - 0.5) * 0.2,
                    0.1
            );

            // Occasionally spawn end rod particles
            if (random.nextInt(20) == 0) {
                world.spawnParticles(
                        ParticleTypes.END_ROD,
                        x, y, z,
                        1, 0, 0.1, 0, 0.05
                );
            }
        }
    }

    /**
     * Handle player interaction (trading)
     */
    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (this.getWorld().isClient) {
            return ActionResult.SUCCESS;
        }

        if (!this.isDead()) {
            if (!tradingAvailable) {
                player.sendMessage(Text.of("§5§oThe Enderman seems distracted..."), false);
                return ActionResult.SUCCESS;
            }

            // Open trading interface
            openTradingScreen(player);
            return ActionResult.SUCCESS;
        }

        return super.interactMob(player, hand);
    }

    /**
     * Open trading screen for the player
     */
    private void openTradingScreen(PlayerEntity player) {
        // Send trading message
        player.sendMessage(Text.of("§5§l[Ender Trader] §7§oI have rare items from the void..."), false);

        // Play trading sound
        this.playSound(SoundEvents.ENTITY_ENDERMAN_AMBIENT, 0.5f, 1.2f);

        // Show available trades in chat (simple implementation)
        showTradesToPlayer(player);
    }

    /**
     * Show available trades to the player in chat
     */
    private void showTradesToPlayer(PlayerEntity player) {
        player.sendMessage(Text.of("§6§l=== Ender Trades ==="), false);

        int i = 1;
        for (Map.Entry<ItemStack, ItemStack> trade : TRADES.entrySet()) {
            ItemStack input = trade.getKey();
            ItemStack output = trade.getValue();

            player.sendMessage(Text.of(String.format("§7%d. §f%dx %s §7-> §a%dx %s",
                    i++,
                    input.getCount(),
                    input.getItem().getName().getString(),
                    output.getCount(),
                    output.getItem().getName().getString()
            )), false);
        }

        player.sendMessage(Text.of("§5§oThrow items near me to trade..."), false);
        player.sendMessage(Text.of("§7§o(Use /summon enderborne:trading_enderman to spawn)"), false);
    }

    /**
     * Check for dropped items nearby and perform trades
     */
    private void checkForTrades() {
        // This would be implemented to check for dropped items
        // and exchange them for the corresponding trade items
        // Simplified for now to avoid complex item handling
    }

    /**
     * Restock trades
     */
    private void restockTrades() {
        tradingAvailable = true;
        restockTimer = 0;

        // Play restock sound
        this.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.0f);

        Enderborne.LOGGER.debug("Trading Enderman restocked at {}", this.getBlockPos());
    }

    /**
     * Override to prevent this enderman from being angry
     */
    @Override
    public void setAngryAt(java.util.UUID angryAt) {
        // Do nothing - trading endermen are always peaceful
    }

    /**
     * Custom name for the trading enderman
     */
    @Override
    public Text getName() {
        return Text.literal("§5Ender Trader");
    }

    /**
     * Always show custom name
     */
    @Override
    public boolean hasCustomName() {
        return true;
    }

    /**
     * Custom name is always visible
     */
    @Override
    public boolean isCustomNameVisible() {
        return true;
    }
}