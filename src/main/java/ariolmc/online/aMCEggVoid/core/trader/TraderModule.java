package ariolmc.online.aMCEggVoid.core.trader;

import ariolmc.online.aMCEggVoid.core.cfg.UniqueItemsCfg;
import lombok.Getter;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

@Getter
public class TraderModule {

    private final TraderCfg cfg;
    private final TraderBoothBuilder boothBuilder;

    public TraderModule(UniqueItemsCfg uniqueItemsCfg) {
        this.cfg = new TraderCfg(uniqueItemsCfg);
        this.boothBuilder = new TraderBoothBuilder(this.cfg);
    }

    public void reloadCfg() {
        this.cfg.load();
    }

    public Villager spawnTraderBooth(Player player) {
        return this.boothBuilder.spawnBooth(player);
    }

    public Villager spawnTraderBoothAt(World world, int cx, int cy, int cz, BlockFace boothFront) {
        return this.boothBuilder.spawnBoothAt(world, cx, cy, cz, boothFront);
    }
}
