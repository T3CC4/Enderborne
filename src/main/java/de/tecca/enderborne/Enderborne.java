// ===== 1. Update Enderborne.java to register the entity =====

package de.tecca.enderborne;

import de.tecca.enderborne.entity.TradingEndermanEntity;
import de.tecca.enderborne.managers.PlayerSpawnManager;
import de.tecca.enderborne.managers.DragonProgressManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Enderborne implements ModInitializer {
	public static final String MOD_ID = "enderborne";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// Entity registration - MUST be static final
	public static final EntityType<TradingEndermanEntity> TRADING_ENDERMAN = Registry.register(
			Registries.ENTITY_TYPE,
			Identifier.of(MOD_ID, "trading_enderman"),
			FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, TradingEndermanEntity::new)
					.dimensions(EntityDimensions.fixed(0.6f, 2.9f)) // Same as Enderman
					.trackRangeChunks(8) // How far clients can see this entity
					.trackedUpdateRate(3) // How often to sync entity data
					.build()
	);

	// Data Attachment Types - Modern way to store persistent player data
	public static final AttachmentType<Boolean> HAS_PLAYED = AttachmentRegistry.createDefaulted(
			Identifier.of(MOD_ID, "has_played"),
			() -> false
	);

	public static final AttachmentType<Boolean> DRAGON_DEFEATED = AttachmentRegistry.createDefaulted(
			Identifier.of(MOD_ID, "dragon_defeated"),
			() -> false
	);

	public static final AttachmentType<Boolean> OVERWORLD_UNLOCKED = AttachmentRegistry.createDefaulted(
			Identifier.of(MOD_ID, "overworld_unlocked"),
			() -> false
	);

	public static final AttachmentType<Long> DEFEAT_TIMESTAMP = AttachmentRegistry.createDefaulted(
			Identifier.of(MOD_ID, "defeat_timestamp"),
			() -> 0L
	);

	public static final AttachmentType<Integer> SPAWN_COUNT = AttachmentRegistry.createDefaulted(
			Identifier.of(MOD_ID, "spawn_count"),
			() -> 0
	);

	// Managers for different aspects of the mod
	private static PlayerSpawnManager spawnManager;
	private static DragonProgressManager dragonManager;

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Enderborne - The End is just the beginning...");

		// Register entity attributes (REQUIRED for living entities)
		registerEntityAttributes();

		// Initialize managers
		spawnManager = new PlayerSpawnManager();
		dragonManager = new DragonProgressManager();

		// Register player events using Fabric API
		registerPlayerEvents();

		LOGGER.info("Enderborne initialized successfully!");
		LOGGER.info("Registered Trading Enderman entity: {}", TRADING_ENDERMAN);
	}

	/**
	 * Register default attributes for custom entities
	 */
	private void registerEntityAttributes() {
		net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
				TRADING_ENDERMAN,
				TradingEndermanEntity.createTradingEndermanAttributes()
		);
	}

	/**
	 * Register all player-related events using Fabric API
	 */
	private void registerPlayerEvents() {
		// Player joins server for the first time or returns
		ServerPlayerEvents.JOIN.register(this::onPlayerJoin);

		// Player respawns after death
		ServerPlayerEvents.AFTER_RESPAWN.register(this::onPlayerRespawn);

		// Player leaves server (for cleanup if needed)
		ServerPlayerEvents.LEAVE.register(this::onPlayerLeave);
	}

	/**
	 * Handle player joining the server
	 */
	private void onPlayerJoin(ServerPlayerEntity player) {
		LOGGER.debug("Player {} joined the server", player.getName().getString());

		// Check if this is the player's first time in Enderborne
		if (!spawnManager.hasPlayedBefore(player)) {
			LOGGER.info("New player {} detected, teleporting to End islands", player.getName().getString());
			spawnManager.teleportToEndIslands(player);
			spawnManager.markPlayerAsPlayed(player);

			// Send welcome message
			spawnManager.sendWelcomeMessage(player);
		} else {
			LOGGER.debug("Returning player {} detected", player.getName().getString());
		}
	}

	/**
	 * Handle player respawning after death
	 */
	private void onPlayerRespawn(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) {
		if (!alive) { // Player died and is respawning
			LOGGER.debug("Player {} died and is respawning", newPlayer.getName().getString());

			// Always respawn in the End until Overworld is unlocked
			if (!dragonManager.hasDefeatedDragon(newPlayer)) {
				spawnManager.teleportToEndIslands(newPlayer);
				spawnManager.sendRespawnMessage(newPlayer);
			}
		}
	}

	/**
	 * Handle player leaving the server (cleanup if needed)
	 */
	private void onPlayerLeave(ServerPlayerEntity player) {
		LOGGER.debug("Player {} left the server", player.getName().getString());
		// Any cleanup logic can go here if needed
	}

	// Getters for managers (for use in other classes)
	public static PlayerSpawnManager getSpawnManager() {
		return spawnManager;
	}

	public static DragonProgressManager getDragonManager() {
		return dragonManager;
	}
}