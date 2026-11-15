package ariolmc.online.aMCEggVoid.core.blazeDrop.listener;

import ariolmc.online.aMCEggVoid.core.blazeDrop.BlazeDropCfg;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class BlazeDeathListener implements Listener {

    private final BlazeDropCfg cfg;

    public BlazeDeathListener(BlazeDropCfg cfg) {
        this.cfg = cfg;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntityType() != EntityType.BLAZE && !(event.getEntity() instanceof Blaze)) {
            return;
        }

        if (!cfg.isEnabled()) {
            return;
        }

        if (cfg.isPlayerKillOnly() && event.getEntity().getKiller() == null) {
            return;
        }

        List<ItemStack> customDrops = cfg.rollDrops();
        if (!customDrops.isEmpty()) {
            event.getDrops().addAll(customDrops);
        }
    }
}
