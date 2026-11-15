package ariolmc.online.aMCEggVoid.core.end;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.boss.DragonBattle;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.logging.Logger;

/**
 * Сервис для очистки и генерации мира Энда:
 * - один слой блоков эндерняка, случайно раскиданных друг от друга
 * - ванильные обсидиановые столбы с кристаллами и рамка выходного портала
 * - спавн ванильного Дракона Края
 */
public class EndGridService {

    private static final int PILLAR_COUNT = 10;
    private static final int PILLAR_RING_RADIUS = 43;
    private static final int EXIT_PORTAL_CLEAR_RADIUS = 4;

    private static final int[] PILLAR_RADII = {2, 3, 2, 3, 2, 3, 2, 3, 2, 3};
    private static final int[] PILLAR_HEIGHTS = {76, 79, 82, 85, 88, 91, 94, 97, 100, 103};
    private static final boolean[] PILLAR_CAGED = {false, true, false, false, true, false, false, false, false, false};

    private final EndCfg cfg;
    private final Logger logger;

    public EndGridService(EndCfg cfg, Logger logger) {
        this.cfg = cfg;
        this.logger = logger;
    }

    public int clearEndArea(World endWorld, int radius) {
        if (endWorld == null) return 0;

        int minY = endWorld.getMinHeight();
        int maxY = endWorld.getMaxHeight() - 1;

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(endWorld))) {
            editSession.setFastMode(true);
            BlockVector3 min = BlockVector3.at(-radius, minY, -radius);
            BlockVector3 max = BlockVector3.at(radius, maxY, radius);
            Region region = new CuboidRegion(min, max);
            int affected = editSession.setBlocks(region, BlockTypes.AIR.getDefaultState());
            editSession.flushQueue();
            return affected;
        } catch (Throwable t) {
            logger.warning("FAWE clearEndArea failed (" + t.getMessage() + "), using Bukkit fallback...");
            return clearEndAreaNative(endWorld, radius, minY, maxY);
        }
    }

    private int clearEndAreaNative(World endWorld, int radius, int minY, int maxY) {
        int cleared = 0;
        int minChunkX = Math.floorDiv(-radius, 16);
        int maxChunkX = Math.floorDiv(radius, 16);
        int minChunkZ = Math.floorDiv(-radius, 16);
        int maxChunkZ = Math.floorDiv(radius, 16);

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                endWorld.loadChunk(cx, cz, true);
                int startX = Math.max(cx * 16, -radius);
                int endX = Math.min(cx * 16 + 15, radius);
                int startZ = Math.max(cz * 16, -radius);
                int endZ = Math.min(cz * 16 + 15, radius);

                for (int x = startX; x <= endX; x++) {
                    for (int z = startZ; z <= endZ; z++) {
                        for (int y = minY; y <= maxY; y++) {
                            if (!endWorld.getBlockAt(x, y, z).getType().isAir()) {
                                endWorld.getBlockAt(x, y, z).setType(Material.AIR, false);
                                cleared++;
                            }
                        }
                    }
                }
            }
        }
        return cleared;
    }

    public int generateEndGrid(World endWorld, int radius) {
        if (endWorld == null) return 0;

        int gridY = cfg.getGridY();
        double scatterChance = cfg.getScatterChance();
        Random random = new Random(endWorld.getSeed() ^ 0x454E44L);
        int placed = 0;

        int minChunkX = Math.floorDiv(-radius, 16);
        int maxChunkX = Math.floorDiv(radius, 16);
        int minChunkZ = Math.floorDiv(-radius, 16);
        int maxChunkZ = Math.floorDiv(radius, 16);

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                endWorld.loadChunk(cx, cz, true);
                int startX = Math.max(cx * 16, -radius);
                int endX = Math.min(cx * 16 + 15, radius);
                int startZ = Math.max(cz * 16, -radius);
                int endZ = Math.min(cz * 16 + 15, radius);

                for (int x = startX; x <= endX; x++) {
                    for (int z = startZ; z <= endZ; z++) {
                        if (x * x + z * z <= EXIT_PORTAL_CLEAR_RADIUS * EXIT_PORTAL_CLEAR_RADIUS) {
                            continue;
                        }
                        if (random.nextDouble() < scatterChance) {
                            endWorld.getBlockAt(x, gridY, z).setType(Material.END_STONE, false);
                            placed++;
                        }
                    }
                }
            }
        }

        if (cfg.isGeneratePillars()) {
            generateExitPortalFrame(endWorld, gridY);
            generateObsidianPillars(endWorld, gridY);
        }

        if (cfg.isSpawnDragon()) {
            ensureEnderDragon(endWorld, gridY);
        }

        return placed;
    }

    /**
     * Строит ванильную рамку выходного портала в (0, baseY, 0),
     * чтобы при убийстве Дракона Края ванильная механика активировала портал и заспавнила яйцо.
     */
    private void generateExitPortalFrame(World endWorld, int baseY) {
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                double distSq = x * x + z * z;
                if (distSq <= 10.5) {
                    if (Math.abs(x) == 3 || Math.abs(z) == 3 || distSq >= 6.5) {
                        endWorld.getBlockAt(x, baseY + 1, z).setType(Material.BEDROCK, false);
                    } else {
                        endWorld.getBlockAt(x, baseY, z).setType(Material.BEDROCK, false);
                    }
                }
            }
        }
        for (int y = baseY + 1; y <= baseY + 4; y++) {
            endWorld.getBlockAt(0, y, 0).setType(Material.BEDROCK, false);
        }
    }

    /**
     * Строит 10 ванильных обсидиановых столбов с кристаллами Края по кругу радиусом 43.
     */
    private void generateObsidianPillars(World endWorld, int baseY) {
        int bottomY = Math.max(endWorld.getMinHeight() + 1, baseY - 20);

        for (int i = 0; i < PILLAR_COUNT; i++) {
            double angle = 2.0 * Math.PI * i / PILLAR_COUNT;
            int centerX = (int) Math.round(PILLAR_RING_RADIUS * Math.cos(angle));
            int centerZ = (int) Math.round(PILLAR_RING_RADIUS * Math.sin(angle));
            int pillarRadius = PILLAR_RADII[i];
            int topY = PILLAR_HEIGHTS[i];

            for (int dx = -pillarRadius; dx <= pillarRadius; dx++) {
                for (int dz = -pillarRadius; dz <= pillarRadius; dz++) {
                    if (dx * dx + dz * dz <= pillarRadius * pillarRadius + 1) {
                        for (int y = bottomY; y <= topY; y++) {
                            endWorld.getBlockAt(centerX + dx, y, centerZ + dz).setType(Material.OBSIDIAN, false);
                        }
                    }
                }
            }

            endWorld.getBlockAt(centerX, topY + 1, centerZ).setType(Material.BEDROCK, false);

            if (PILLAR_CAGED[i]) {
                buildCrystalCage(endWorld, centerX, topY + 1, centerZ);
            }

            Location crystalLoc = new Location(endWorld, centerX + 0.5, topY + 2, centerZ + 0.5);
            endWorld.getEntitiesByClass(EnderCrystal.class).stream()
                    .filter(c -> c.getLocation().distanceSquared(crystalLoc) < 9.0)
                    .forEach(EnderCrystal::remove);

            EnderCrystal crystal = (EnderCrystal) endWorld.spawnEntity(crystalLoc, EntityType.END_CRYSTAL);
            crystal.setShowingBottom(true);
        }
    }

    private void buildCrystalCage(World endWorld, int cx, int baseY, int cz) {
        for (int dy = 0; dy <= 3; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    boolean isWall = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                    boolean isRoof = dy == 3;
                    if (isWall || isRoof) {
                        endWorld.getBlockAt(cx + dx, baseY + dy, cz + dz).setType(Material.IRON_BARS, false);
                    }
                }
            }
        }
    }

    private void ensureEnderDragon(World endWorld, int baseY) {
        DragonBattle battle = endWorld.getEnderDragonBattle();
        if (battle != null) {
            if (battle.getEnderDragon() == null) {
                if (battle.hasBeenPreviouslyKilled()) {
                    battle.initiateRespawn();
                } else {
                    endWorld.spawnEntity(new Location(endWorld, 0.5, Math.max(baseY + 35, 95), 0.5), EntityType.ENDER_DRAGON);
                }
            }
        } else {
            endWorld.spawnEntity(new Location(endWorld, 0.5, Math.max(baseY + 35, 95), 0.5), EntityType.ENDER_DRAGON);
        }
    }

    public int prepareAndGenerate(World endWorld, int genRadius, int clearRadius, Player notifyPlayer) {
        int clearRad = Math.max(genRadius, clearRadius);
        if (notifyPlayer != null) {
            notifyPlayer.sendMessage("§6[Энд] §eОчистка зоны радиусом " + clearRad + " блоков (FAWE)...");
        }
        int cleared = clearEndArea(endWorld, clearRad);
        if (notifyPlayer != null) {
            notifyPlayer.sendMessage("§6[Энд] §aОчищено блоков: §f" + cleared + "§a. Генерация Энда (" + genRadius + "x" + genRadius + ")...");
        }
        int placed = generateEndGrid(endWorld, genRadius);
        if (notifyPlayer != null) {
            notifyPlayer.sendMessage("§6[Энд] §aГотово! Установлено блоков эндерняка: §f" + placed + "§a, столбы и дракон активны.");
        }
        return placed;
    }
}
