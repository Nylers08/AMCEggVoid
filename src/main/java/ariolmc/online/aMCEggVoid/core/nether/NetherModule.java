package ariolmc.online.aMCEggVoid.core.nether;

import ariolmc.online.aMCEggVoid.core.cfg.LootCfg;
import ariolmc.online.aMCEggVoid.core.cfg.UniqueItemsCfg;
import ariolmc.online.aMCEggVoid.core.nether.listener.SpawnerProtectListener;
import lombok.Getter;
import org.bukkit.World;

import java.util.List;

@Getter
public class NetherModule {

    private final NetherCfg cfg;
    private final NetherChestCfg chestCfg;
    private final NetherGridService gridService;
    private final NetherGridChunkGenerator chunkGenerator;
    private final SpawnerProtectListener spawnerProtectListener;

    public NetherModule() {
        this(null, null);
    }

    public NetherModule(UniqueItemsCfg uniqueItemsCfg) {
        this(uniqueItemsCfg, null);
    }

    public NetherModule(UniqueItemsCfg uniqueItemsCfg, LootCfg lootCfg) {
        this.cfg = new NetherCfg();
        this.cfg.load();

        this.chestCfg = new NetherChestCfg(uniqueItemsCfg, lootCfg);
        this.chestCfg.load();

        this.gridService = new NetherGridService(this.cfg, this.chestCfg);
        this.chunkGenerator = new NetherGridChunkGenerator(this.cfg, this.gridService);
        this.spawnerProtectListener = new SpawnerProtectListener();
    }

    public void reloadCfg() {
        this.cfg.load();
        this.chestCfg.load();
    }

    public int generateNether(World world, int radius) {
        return this.gridService.generateNetherArea(world, radius);
    }

    public int clearNether(World world, int radius) {
        return this.gridService.clearNetherArea(world, radius);
    }

    public List<BlazeZone> getActiveZones() {
        return this.gridService.getActiveZones();
    }

    public int getLastGeneratedChestsCount() {
        return this.gridService.getLastGeneratedChestsCount();
    }
}
