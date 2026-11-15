package ariolmc.online.aMCEggVoid.core.nether;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class NetherGridChunkGenerator extends ChunkGenerator {

    private final NetherCfg cfg;
    private final NetherGridService gridService;

    public NetherGridChunkGenerator(NetherCfg cfg, NetherGridService gridService) {
        this.cfg = cfg;
        this.gridService = gridService;
    }

    @Override
    public boolean shouldGenerateNoise() {
        return false;
    }

    @Override
    public boolean shouldGenerateBedrock() {
        return false;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return true;
    }

    @Override
    public void generateSurface(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        int baseY = cfg.getGridY();
        int minWorldX = chunkX << 4;
        int minWorldZ = chunkZ << 4;

        for (int x = 0; x < 16; x++) {
            int worldX = minWorldX + x;
            for (int z = 0; z < 16; z++) {
                int worldZ = minWorldZ + z;

                BlazeZone zone = gridService.getBlazeZoneAt(worldX, worldZ);
                if (zone != null) {
                    Material floorMat = zone.getType().getFloorMaterial();
                    // Зона спавна: незерские кирпичи (ифриты) или золотые блоки (пиглины) с дистанцией 1 блок (шаг 2)
                    if (zone.isGridBlock(worldX, worldZ)) {
                        chunkData.setBlock(x, baseY, z, floorMat);
                    }
                    // Спавнер строго по центру
                    if (worldX == zone.getCenterX() && worldZ == zone.getCenterZ()) {
                        chunkData.setBlock(x, baseY, z, floorMat);
                        chunkData.setBlock(x, baseY + 1, z, Material.SPAWNER);
                    }
                } else {
                    // Основной ад: сетка из незерака с дистанцией 2 блока (шаг 3)
                    if (Math.floorMod(worldX, cfg.getMainGridStep()) == 0 && Math.floorMod(worldZ, cfg.getMainGridStep()) == 0) {
                        chunkData.setBlock(x, baseY, z, Material.NETHERRACK);
                    }
                }
            }
        }
    }
}
