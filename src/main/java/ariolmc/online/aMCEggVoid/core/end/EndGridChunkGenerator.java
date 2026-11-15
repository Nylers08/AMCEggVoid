package ariolmc.online.aMCEggVoid.core.end;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * Генератор чанков для Энда:
 * одиночный слой случайно раскиданных блоков эндерняка на высоте grid-y.
 */
public class EndGridChunkGenerator extends ChunkGenerator {

    private final EndCfg cfg;

    public EndGridChunkGenerator(EndCfg cfg) {
        this.cfg = cfg;
    }

    @Override
    public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        int gridY = cfg.getGridY();
        if (gridY < chunkData.getMinHeight() || gridY >= chunkData.getMaxHeight()) {
            return;
        }

        double chance = cfg.getScatterChance();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkX * 16 + x;
                int worldZ = chunkZ * 16 + z;
                if (worldX * worldX + worldZ * worldZ <= 16) {
                    continue;
                }
                if (random.nextDouble() < chance) {
                    chunkData.setBlock(x, gridY, z, Material.END_STONE);
                }
            }
        }
    }

    @Override
    public boolean shouldGenerateNoise() {
        return false;
    }

    @Override
    public boolean shouldGenerateSurface() {
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
    public boolean shouldGenerateStructures() {
        return false;
    }
}
