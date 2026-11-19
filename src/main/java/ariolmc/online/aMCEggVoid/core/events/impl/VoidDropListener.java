package ariolmc.online.aMCEggVoid.core.events.impl;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

/**
 * Слушатель выбрасывания предметов игроком для мини-ивента «Жертва Бездне».
 */
public class VoidDropListener implements Listener {

    private final VoidDropMiniEvent voidDropMiniEvent;

    public VoidDropListener(VoidDropMiniEvent voidDropMiniEvent) {
        this.voidDropMiniEvent = voidDropMiniEvent;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!voidDropMiniEvent.isActive()) {
            return;
        }
        Player player = event.getPlayer();
        voidDropMiniEvent.trackDrop(player, event.getItemDrop());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!voidDropMiniEvent.isActive()) {
            return;
        }
        voidDropMiniEvent.untrackDrop(event.getItem().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemMerge(ItemMergeEvent event) {
        if (!voidDropMiniEvent.isActive()) {
            return;
        }
        if (voidDropMiniEvent.isTrackedDrop(event.getEntity().getUniqueId())
                || voidDropMiniEvent.isTrackedDrop(event.getTarget().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
