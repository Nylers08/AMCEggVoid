package ariolmc.online.aMCEggVoid.core.trader;

import ariolmc.online.aMCEggVoid.core.cfg.UniqueItemsCfg;
import lombok.Getter;
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
}
