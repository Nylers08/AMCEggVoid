package ariolmc.online.aMCEggVoid.core.piglinBarter;

import ariolmc.online.aMCEggVoid.core.cfg.LootCfg;
import ariolmc.online.aMCEggVoid.core.cfg.UniqueItemsCfg;
import ariolmc.online.aMCEggVoid.core.piglinBarter.listener.PiglinBarterListener;
import lombok.Getter;

@Getter
public class PiglinBarterModule {

    private final PiglinBarterCfg cfg;
    private final PiglinBarterListener barterListener;

    public PiglinBarterModule(UniqueItemsCfg uniqueItemsCfg, LootCfg lootCfg) {
        this.cfg = new PiglinBarterCfg(uniqueItemsCfg, lootCfg);
        this.barterListener = new PiglinBarterListener(this.cfg);
    }

    public void reloadCfg() {
        this.cfg.load();
    }
}
