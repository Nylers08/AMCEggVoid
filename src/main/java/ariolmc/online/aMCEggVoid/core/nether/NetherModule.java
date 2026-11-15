package ariolmc.online.aMCEggVoid.core.nether;

import lombok.Getter;
import org.bukkit.World;

import java.util.List;

@Getter
public class NetherModule {

    private final NetherCfg cfg;
    private final NetherGridService gridService;
    private final NetherGridChunkGenerator chunkGenerator;

    public NetherModule() {
        this.cfg = new NetherCfg();
        this.cfg.load();

        this.gridService = new NetherGridService(this.cfg);
        this.chunkGenerator = new NetherGridChunkGenerator(this.cfg, this.gridService);
    }

    public void reloadCfg() {
        this.cfg.load();
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
}
