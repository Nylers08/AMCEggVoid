package ariolmc.online.aMCEggVoid.core.blazeDrop;

import ariolmc.online.aMCEggVoid.core.blazeDrop.listener.BlazeDeathListener;
import ariolmc.online.aMCEggVoid.core.cfg.UniqueItemsCfg;
import lombok.Getter;

@Getter
public class BlazeDropModule {

    private final BlazeDropCfg cfg;
    private final BlazeDeathListener deathListener;

    public BlazeDropModule(UniqueItemsCfg uniqueItemsCfg) {
        this.cfg = new BlazeDropCfg(uniqueItemsCfg);
        this.deathListener = new BlazeDeathListener(this.cfg);
    }

    public void reloadCfg() {
        this.cfg.load();
    }
}
