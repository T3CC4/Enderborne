package de.tecca.enderborne.corruption;

import de.tecca.enderborne.Enderborne;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Random;

/**
 * Manages sculk corruption spreading throughout dimensions
 * End: Heavy corruption (60-80%)
 * Nether: Medium corruption (30-50%)
 * Overworld: Light corruption (10-20%)
 */
public class SculkCorruptionManager {

    private static final Random RANDOM = new Random();

    // Corruption blocks to use (all vanilla blocks for Vanilla+ feel)
    private static final Block[] CORRUPTION_BLOCKS = {
            Blocks.SCULK,
            Blocks.SCULK_VEIN,
            Blocks.SCULK_CATALYST,
            Blocks.SCULK_SENSOR,
            Blocks.SCULK_SHRIEKER
    };

    private static final Block[] SPREADABLE_BLOCKS = {
            Blocks.STONE, Blocks.DEEPSLATE, Blocks.END_STONE,
            Blocks.NETHERRACK, Blocks.BLACKSTONE, Blocks.BASALT,
            Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT
    };

    /**
     * Apply corruption to a chunk during world generation or chunk loading
     */
    public static void applyChunkCorruption(ServerWorld world, ChunkPos chunkPos) {
        double corruptionChance = getCorruptionChance(world);

        if (RANDOM.nextDouble() > corruptionChance) {
            return; // Skip this chunk
        }

        WorldChunk chunk = world.getChunk(chunkPos.x, chunkPos.z);
        BlockPos chunkStart = chunkPos.getStartPos();

        // Apply corruption in patches throughout the chunk
        int patchCount = RANDOM.nextInt(3) + 1; // 1-3 patches per chunk

        for (int patch = 0; patch < patchCount; patch++) {
            int x = RANDOM.nextInt(16);
            int z = RANDOM.nextInt(16);
            int y = findSuitableY(world, chunkStart.add(x, 0, z));

            if (y > 0) {
                BlockPos centerPos = chunkStart.add(x, y, z);
                createCorruptionPatch(world, centerPos);
            }
        }
    }

    /**
     * Get corruption chance based on dimension
     */
    private static double getCorruptionChance(World world) {
        if (world.getRegistryKey() == World.END) {
            return 0.75; // 75% chance in End (heavy corruption)
        } else if (world.getRegistryKey() == World.NETHER) {
            return 0.40; // 40% chance in Nether (medium corruption)
        } else if (world.getRegistryKey() == World.OVERWORLD) {
            return 0.15; // 15% chance in Overworld (light corruption)
        }
        return 0.0;
    }

    /**
     * Find a suitable Y level for corruption placement
     */
    private static int findSuitableY(ServerWorld world, BlockPos pos) {
        // Search for solid ground using proper method
        int topY = world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, pos.getX(), pos.getZ());
        int bottomY = world.getBottomY();

        for (int y = topY; y > bottomY; y--) {
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
            BlockState state = world.getBlockState(checkPos);

            if (!state.isAir() && canCorrupt(state.getBlock())) {
                return y;
            }
        }
        return -1; // No suitable location found
    }

    /**
     * Create a patch of corruption at the given location
     */
    private static void createCorruptionPatch(ServerWorld world, BlockPos center) {
        int patchSize = RANDOM.nextInt(4) + 2; // 2-5 block radius
        double intensity = getCorruptionIntensity(world);

        for (int x = -patchSize; x <= patchSize; x++) {
            for (int z = -patchSize; z <= patchSize; z++) {
                for (int y = -1; y <= 2; y++) {
                    BlockPos pos = center.add(x, y, z);

                    // Distance-based corruption probability
                    double distance = Math.sqrt(x*x + y*y + z*z);
                    double corruptChance = Math.max(0, intensity * (1.0 - (distance / patchSize)));

                    if (RANDOM.nextDouble() < corruptChance) {
                        tryCorruptBlock(world, pos);
                    }
                }
            }
        }

        // Add a sculk catalyst at the center for spreading mechanics
        if (RANDOM.nextDouble() < 0.3) { // 30% chance for catalyst
            if (canPlaceCorruption(world, center)) {
                world.setBlockState(center, Blocks.SCULK_CATALYST.getDefaultState());
            }
        }
    }

    /**
     * Get corruption intensity based on dimension
     */
    private static double getCorruptionIntensity(World world) {
        if (world.getRegistryKey() == World.END) {
            return 0.8; // 80% intensity in End
        } else if (world.getRegistryKey() == World.NETHER) {
            return 0.5; // 50% intensity in Nether
        } else if (world.getRegistryKey() == World.OVERWORLD) {
            return 0.3; // 30% intensity in Overworld
        }
        return 0.0;
    }

    /**
     * Attempt to corrupt a single block
     */
    private static void tryCorruptBlock(ServerWorld world, BlockPos pos) {
        if (!canPlaceCorruption(world, pos)) {
            return;
        }

        BlockState currentState = world.getBlockState(pos);
        Block corruptionBlock = chooseCorruptionBlock(world, currentState.getBlock());

        if (corruptionBlock != null) {
            // Choose appropriate corruption type
            if (corruptionBlock == Blocks.SCULK_VEIN && !currentState.isAir()) {
                // Sculk veins can be placed on existing blocks
                placeCorruptionVein(world, pos);
            } else if (canReplace(currentState.getBlock())) {
                // Replace the block entirely
                world.setBlockState(pos, corruptionBlock.getDefaultState());
            }
        }
    }

    /**
     * Choose appropriate corruption block based on dimension and existing block
     */
    private static Block chooseCorruptionBlock(World world, Block existingBlock) {
        if (world.getRegistryKey() == World.END) {
            // End prefers more dramatic corruption
            return CORRUPTION_BLOCKS[RANDOM.nextInt(CORRUPTION_BLOCKS.length)];
        } else if (world.getRegistryKey() == World.NETHER) {
            // Nether prefers veins and sensors
            Block[] netherCorruption = {Blocks.SCULK_VEIN, Blocks.SCULK, Blocks.SCULK_SENSOR};
            return netherCorruption[RANDOM.nextInt(netherCorruption.length)];
        } else {
            // Overworld prefers subtle corruption
            Block[] overworldCorruption = {Blocks.SCULK_VEIN, Blocks.SCULK};
            return overworldCorruption[RANDOM.nextInt(overworldCorruption.length)];
        }
    }

    /**
     * Place sculk veins on existing blocks
     */
    private static void placeCorruptionVein(ServerWorld world, BlockPos pos) {
        // Sculk veins can be placed on the surface of blocks
        BlockState veinState = Blocks.SCULK_VEIN.getDefaultState();
        world.setBlockState(pos.up(), veinState);
    }

    /**
     * Check if corruption can be placed at this position
     */
    private static boolean canPlaceCorruption(ServerWorld world, BlockPos pos) {
        return world.isInBuildLimit(pos) &&
                !world.getBlockState(pos).isLiquid() &&
                world.getWorldBorder().contains(pos);
    }

    /**
     * Check if a block can be corrupted/replaced
     */
    private static boolean canCorrupt(Block block) {
        for (Block spreadable : SPREADABLE_BLOCKS) {
            if (block == spreadable) return true;
        }
        return false;
    }

    /**
     * Check if a block can be replaced by corruption
     */
    private static boolean canReplace(Block block) {
        return block == Blocks.AIR ||
                block == Blocks.CAVE_AIR ||
                canCorrupt(block);
    }

    /**
     * Spread corruption naturally over time (called periodically)
     */
    public static void spreadCorruptionNaturally(ServerWorld world) {
        if (RANDOM.nextDouble() < 0.1) { // 10% chance per call
            // Find random sculk catalyst and spread from it
            ChunkPos randomChunk = getRandomLoadedChunk(world);
            if (randomChunk != null) {
                findAndSpreadFromCatalyst(world, randomChunk);
            }
        }
    }

    /**
     * Get a random loaded chunk
     */
    private static ChunkPos getRandomLoadedChunk(ServerWorld world) {
        // Simple implementation - in practice you'd want to track loaded chunks
        int x = RANDOM.nextInt(200) - 100; // -100 to 100
        int z = RANDOM.nextInt(200) - 100;
        return new ChunkPos(x, z);
    }

    /**
     * Find sculk catalysts in a chunk and spread corruption from them
     */
    private static void findAndSpreadFromCatalyst(ServerWorld world, ChunkPos chunkPos) {
        BlockPos start = chunkPos.getStartPos();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = world.getBottomY(); y < world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, start.getX(), start.getZ()); y++) {
                    BlockPos pos = start.add(x, y, z);
                    if (world.getBlockState(pos).getBlock() == Blocks.SCULK_CATALYST) {
                        // Spread corruption from this catalyst
                        spreadFromCatalyst(world, pos);
                        return; // Only spread from one catalyst per call
                    }
                }
            }
        }
    }

    /**
     * Spread corruption from a sculk catalyst
     */
    private static void spreadFromCatalyst(ServerWorld world, BlockPos catalystPos) {
        int range = 3; // 3 block range

        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                for (int y = -range; y <= range; y++) {
                    if (RANDOM.nextDouble() < 0.1) { // 10% chance per block
                        BlockPos targetPos = catalystPos.add(x, y, z);
                        tryCorruptBlock(world, targetPos);
                    }
                }
            }
        }

        Enderborne.LOGGER.debug("Spread corruption from catalyst at {}", catalystPos);
    }
}