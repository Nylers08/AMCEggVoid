package ariolmc.online.aMCEggVoid.core.victory;

import ariolmc.online.aMCEggVoid.core.EventState;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;

/**
 * Слушатель подбора Яйца Дракона игроком для запуска поздравления победителя.
 */
public class VictoryListener implements Listener {

    private final VictoryService victoryService;

    public VictoryListener(VictoryService victoryService) {
        this.victoryService = victoryService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDragonEggPickup(EntityPickupItemEvent event) {
        if (!EventState.isRunning() || victoryService.isVictoryTriggered()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getItem().getItemStack().getType() != Material.DRAGON_EGG) {
            return;
        }

        victoryService.triggerVictory(player);
    }
}
