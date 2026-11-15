package ariolmc.online.aMCEggVoid.core.nether;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class NetherGridService {

    private final NetherCfg cfg;
    private final NetherChestCfg chestCfg;
    private final List<BlazeZone> activeZones = new ArrayList<>();
    private int lastGeneratedChestsCount = 0;

    public NetherGridService(NetherCfg cfg) {
        this(cfg, null);
    }

    public NetherGridService(NetherCfg cfg, NetherChestCfg chestCfg) {
        this.cfg = cfg;
        this.chestCfg = chestCfg;
    }

    public int getLastGeneratedChestsCount() {
        return lastGeneratedChestsCount;
    }

    public List<BlazeZone> getActiveZones() {
        return activeZones;
    }

    public BlazeZone getBlazeZoneAt(int x, int z) {
        for (BlazeZone zone : activeZones) {
            if (zone.contains(x, z)) {
                return zone;
            }
        }
        return null;
    }

    /**
     * Очищает старые блоки в указанном радиусе через FastAsyncWorldEdit (FAWE) с откатом на нативный обход.
     */
    public int clearNetherArea(World world, int radius) {
        if (radius <= 0) {
            return 0;
        }

        try {
            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
            int minY = world.getMinHeight();
            int maxY = world.getMaxHeight();

            BlockVector3 min = BlockVector3.at(-radius, minY, -radius);
            BlockVector3 max = BlockVector3.at(radius, maxY - 1, radius);
            CuboidRegion region = new CuboidRegion(weWorld, min, max);

            try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                editSession.setBlocks((com.sk89q.worldedit.regions.Region) region, BlockTypes.AIR.getDefaultState());
                editSession.flushQueue();
            }

            return (int) region.getVolume();
        } catch (Throwable t) {
            return clearNetherAreaNative(world, radius);
        }
    }

    /**
     * Нативная пакетная очистка чанков (fallback).
     */
    public int clearNetherAreaNative(World world, int radius) {
        int blocksCleared = 0;
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();

        int minChunkX = (-radius) >> 4;
        int maxChunkX = radius >> 4;
        int minChunkZ = (-radius) >> 4;
        int maxChunkZ = radius >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                Chunk chunk = world.getChunkAt(cx, cz);

                int startX = Math.max(-radius, cx << 4);
                int endX = Math.min(radius, (cx << 4) + 15);
                int startZ = Math.max(-radius, cz << 4);
                int endZ = Math.min(radius, (cz << 4) + 15);

                int localStartX = startX - (cx << 4);
                int localEndX = endX - (cx << 4);
                int localStartZ = startZ - (cz << 4);
                int localEndZ = endZ - (cz << 4);

                for (int lx = localStartX; lx <= localEndX; lx++) {
                    for (int lz = localStartZ; lz <= localEndZ; lz++) {
                        for (int y = minY; y < maxY; y++) {
                            Block block = chunk.getBlock(lx, y, lz);
                            if (!block.getType().isAir()) {
                                block.setType(Material.AIR, false);
                                blocksCleared++;
                            }
                        }
                    }
                }
            }
        }
        return blocksCleared;
    }

    /**
     * Вычисляет нечетный размер грани зоны ифритов в зависимости от радиуса генерации.
     */
    public int calculateBlocksOnEdge(int radius) {
        int minBlocks = cfg.getMinBlocksOnEdge();
        int maxBlocks = cfg.getMaxBlocksOnEdge();

        // Скейлинг: от minBlocks до maxBlocks в зависимости от радиуса (например радиус 30 -> 5, 80 -> 9, 150 -> 15)
        int scaled = minBlocks + 2 * (int) Math.max(0, (radius - 30) / 20.0);
        if (scaled > maxBlocks) scaled = maxBlocks;
        if (scaled < minBlocks) scaled = minBlocks;

        // Гарантируем нечетность
        if (scaled % 2 == 0) {
            scaled--;
        }
        if (scaled < 3) scaled = 3;

        return scaled;
    }

    /**
     * Создает список случайных зон ифритов, не перекрывающих друг друга.
     */
    public List<BlazeZone> distributeBlazeZones(int radius) {
        List<BlazeZone> zones = new ArrayList<>();
        int blocksOnEdge = calculateBlocksOnEdge(radius);
        int halfBlocks = (blocksOnEdge - 1) / 2;
        int halfSpan = halfBlocks * 2;
        int padding = cfg.getZonePadding();

        // Сколько зон может позволить радиус
        int allowedByRadius = Math.max(1, radius / 40);
        int targetZones = Math.min(allowedByRadius, cfg.getMaxZones());

        int minCoord = -radius + halfSpan + 10;
        int maxCoord = radius - halfSpan - 10;
        if (minCoord > maxCoord) {
            minCoord = -radius + halfSpan;
            maxCoord = radius - halfSpan;
        }

        Random random = new Random();

        for (int i = 0; i < targetZones; i++) {
            boolean placed = false;
            for (int attempt = 0; attempt < 100; attempt++) {
                int cx = minCoord == maxCoord ? minCoord : minCoord + random.nextInt(maxCoord - minCoord + 1);
                int cz = minCoord == maxCoord ? minCoord : minCoord + random.nextInt(maxCoord - minCoord + 1);

                // Выравниваем центр по четным координатам
                cx = (cx / 2) * 2;
                cz = (cz / 2) * 2;

                BlazeZone candidate = new BlazeZone(cx, cz, blocksOnEdge);

                boolean overlaps = false;
                for (BlazeZone existing : zones) {
                    if (candidate.overlaps(existing, padding)) {
                        overlaps = true;
                        break;
                    }
                }

                if (!overlaps) {
                    zones.add(candidate);
                    placed = true;
                    break;
                }
            }
            if (!placed && zones.isEmpty()) {
                // Если не удалось разместить со свободным местом, хотя бы одну зону ставим по центру
                zones.add(new BlazeZone(0, 0, blocksOnEdge));
            }
        }

        return zones;
    }

    /**
     * Генерирует сетку Незера, зоны ифритов со спавнерами и сундуки со случайным лутом.
     */
    public int generateNetherArea(World world, int radius) {
        int baseY = cfg.getGridY();
        int blocksPlaced = 0;
        this.lastGeneratedChestsCount = 0;

        // Распределяем зоны спавна ифритов
        this.activeZones.clear();
        this.activeZones.addAll(distributeBlazeZones(radius));

        List<int[]> netherrackCoords = new ArrayList<>();

        int minX = -radius;
        int maxX = radius;
        int minZ = -radius;
        int maxZ = radius;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlazeZone zone = getBlazeZoneAt(x, z);

                if (zone != null) {
                    // Зона ифритов: незерские кирпичи с дистанцией 1 блок (шаг 2)
                    if (zone.isGridBlock(x, z)) {
                        Block block = world.getBlockAt(x, baseY, z);
                        if (block.getType() != Material.NETHER_BRICKS) {
                            block.setType(Material.NETHER_BRICKS, false);
                            blocksPlaced++;
                        }
                    }
                } else {
                    // Основная зона: незерак с дистанцией 2 блока (шаг 3)
                    if (Math.floorMod(x, cfg.getMainGridStep()) == 0 && Math.floorMod(z, cfg.getMainGridStep()) == 0) {
                        Block block = world.getBlockAt(x, baseY, z);
                        if (block.getType() != Material.NETHERRACK) {
                            block.setType(Material.NETHERRACK, false);
                            blocksPlaced++;
                        }
                        if (x != 0 || z != 0) {
                            netherrackCoords.add(new int[]{x, z});
                        }
                    }
                }
            }
        }

        // Спавним спавнер строго по центру каждой зоны
        for (BlazeZone zone : activeZones) {
            int cx = zone.getCenterX();
            int cz = zone.getCenterZ();

            world.getBlockAt(cx, baseY, cz).setType(Material.NETHER_BRICKS, false);

            Block spawnerBlock = world.getBlockAt(cx, baseY + 1, cz);
            spawnerBlock.setType(Material.SPAWNER, false);
            if (spawnerBlock.getState() instanceof CreatureSpawner spawner) {
                spawner.setSpawnedType(EntityType.BLAZE);
                spawner.update();
            }
        }

        // Генерируем сундуки с лутом на блоках сетки
        generateChests(world, baseY, netherrackCoords);

        return blocksPlaced;
    }

    private void generateChests(World world, int baseY, List<int[]> netherrackCoords) {
        if (chestCfg == null || !chestCfg.isEnabled() || netherrackCoords.isEmpty() || chestCfg.getLoot().isEmpty()) {
            return;
        }

        Random random = new Random();
        int minC = Math.max(0, chestCfg.getMinChests());
        int maxC = Math.max(minC, chestCfg.getMaxChests());
        int targetChests = minC == maxC ? minC : (minC + random.nextInt(maxC - minC + 1));
        targetChests = Math.min(targetChests, netherrackCoords.size());

        if (targetChests <= 0) {
            return;
        }

        Collections.shuffle(netherrackCoords, random);
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};

        for (int i = 0; i < targetChests; i++) {
            int[] pos = netherrackCoords.get(i);
            int cx = pos[0];
            int cz = pos[1];

            Block chestBlock = world.getBlockAt(cx, baseY + 1, cz);
            chestBlock.setType(Material.CHEST, false);
            if (chestBlock.getBlockData() instanceof Directional directional) {
                directional.setFacing(faces[random.nextInt(faces.length)]);
                chestBlock.setBlockData(directional, false);
            }

            if (chestBlock.getState() instanceof Chest chest) {
                fillChest(chest, random);
                lastGeneratedChestsCount++;
            }
        }
    }

    private void fillChest(Chest chest, Random random) {
        Inventory inv = chest.getBlockInventory();
        inv.clear();

        List<ItemStack> items = chestCfg.rollChestContents();
        if (items.isEmpty()) {
            return;
        }

        List<Integer> slots = new ArrayList<>(inv.getSize());
        for (int s = 0; s < inv.getSize(); s++) {
            slots.add(s);
        }
        Collections.shuffle(slots, random);

        int idx = 0;
        for (ItemStack item : items) {
            if (idx >= slots.size()) {
                break;
            }
            inv.setItem(slots.get(idx++), item);
        }
    }
}
