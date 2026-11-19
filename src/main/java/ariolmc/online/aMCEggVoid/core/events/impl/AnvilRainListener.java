package ariolmc.online.aMCEggVoid.core.events.impl;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDropItemEvent;

/**
 * Слушатель приземления и поломки наковален мини-ивента «Наковальный Дождь».
 * Гарантирует мгновенное исчезновение наковальни без установки блока и без выпадения предмета.
 */
public class AnvilRainListener implements Listener {

    private final AnvilRainMiniEvent anvilRainMiniEvent;

    public AnvilRainListener(AnvilRainMiniEvent anvilRainMiniEvent) {
        this.anvilRainMiniEvent = anvilRainMiniEvent;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAnvilLand(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock fb)) {
            return;
        }
        if (!anvilRainMiniEvent.isTrackedAnvil(fb.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        anvilRainMiniEvent.untrackAnvil(fb.getUniqueId());
        fb.getWorld().spawnParticle(Particle.SMOKE, fb.getLocation().add(0, 0.3, 0), 8, 0.25, 0.1, 0.25, 0.02);
        fb.getWorld().playSound(fb.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.6f, 1.0f);
        fb.remove();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAnvilBreakToDrop(EntityDropItemEvent event) {
        if (!(event.getEntity() instanceof FallingBlock fb)) {
            return;
        }
        if (!anvilRainMiniEvent.isTrackedAnvil(fb.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        event.getItemDrop().remove();
        anvilRainMiniEvent.untrackAnvil(fb.getUniqueId());
        fb.getWorld().spawnParticle(Particle.SMOKE, fb.getLocation().add(0, 0.3, 0), 8, 0.25, 0.1, 0.25, 0.02);
        fb.getWorld().playSound(fb.getLocation(), Sound.BLOCK_ANVIL_DESTROY, 0.6f, 1.0f);
        fb.remove();
    }
}
