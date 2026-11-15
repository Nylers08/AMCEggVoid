package ariolmc.online.aMCEggVoid.core.piglinBarter.listener;

import ariolmc.online.aMCEggVoid.core.EventState;
import ariolmc.online.aMCEggVoid.core.piglinBarter.PiglinBarterCfg;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PiglinBarterEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class PiglinBarterListener implements Listener {

    private final PiglinBarterCfg cfg;

    public PiglinBarterListener(PiglinBarterCfg cfg) {
        this.cfg = cfg;
    }

    @EventHandler
    public void onPiglinBarter(PiglinBarterEvent event) {
        if (!EventState.isRunning()) {
            return;
        }
        if (!cfg.isEnabled()) {
            return;
        }

        List<ItemStack> customDrops = cfg.rollBarterDrops();
        if (customDrops.isEmpty()) {
            return;
        }

        if (cfg.isReplaceVanilla()) {
            event.getOutcome().clear();
        }
        event.getOutcome().addAll(customDrops);
    }
}
