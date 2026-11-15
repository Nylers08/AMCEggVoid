package ariolmc.online.aMCEggVoid.core.nether.listener;

import ariolmc.online.aMCEggVoid.core.EventState;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class SpawnerProtectListener implements Listener {

    private boolean isProtectedSpawner(Block block) {
        if (!EventState.isRunning()) {
            return false;
        }
        if (block == null || block.getType() != Material.SPAWNER) {
            return false;
        }
        if (block.getState() instanceof CreatureSpawner spawner) {
            EntityType type = spawner.getSpawnedType();
            return type == EntityType.BLAZE || type == EntityType.PIGLIN;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (isProtectedSpawner(event.getBlock())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§6AMCEggVoid | §cЭтот спавнер нельзя разрушить!");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(this::isProtectedSpawner);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(this::isProtectedSpawner);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (isProtectedSpawner(event.getBlock())) {
            event.setCancelled(true);
        }
    }
}
