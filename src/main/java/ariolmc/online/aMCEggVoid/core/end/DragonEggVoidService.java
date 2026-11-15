package ariolmc.online.aMCEggVoid.core.end;

import ariolmc.online.aMCEggVoid.core.EventState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Logger;

/**
 * Отслеживает падение Яйца Дракона (как FallingBlock или выброшенного Item) в пустоту
 * и возрождает его на бедроке в Обычном мире (Overworld).
 */
public class DragonEggVoidService implements Listener {

    private static final int SEARCH_RADIUS = 48;

    private final Plugin plugin;
    private final Logger logger;
    private BukkitTask checkTask;
    private long lastRespawnTick = -100L;

    public DragonEggVoidService(Plugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    public void start() {
        stop();
        checkTask = Bukkit.getScheduler().runTaskTimer(plugin, this::scanVoidEggs, 5L, 5L);
    }

    public void stop() {
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDragonEggItemVoidDamage(EntityDamageEvent event) {
        if (!EventState.isRunning()) {
            return;
        }
        if (event.getCause() != EntityDamageEvent.DamageCause.VOID) {
            return;
        }
        if (!(event.getEntity() instanceof Item item)) {
            return;
        }
        if (item.getItemStack().getType() != Material.DRAGON_EGG) {
            return;
        }

        event.setCancelled(true);
        item.remove();
        respawnEggOnOverworldBedrock();
    }

    private void scanVoidEggs() {
        if (!EventState.isRunning()) {
            return;
        }
        for (World world : Bukkit.getWorlds()) {
            int minY = world.getMinHeight();
            for (Entity entity : world.getEntities()) {
                if (entity.getLocation().getY() >= minY) {
                    continue;
                }

                if (entity instanceof Item item && item.getItemStack().getType() == Material.DRAGON_EGG) {
                    item.remove();
                    respawnEggOnOverworldBedrock();
                } else if (entity instanceof FallingBlock falling
                        && falling.getBlockData().getMaterial() == Material.DRAGON_EGG) {
                    falling.remove();
                    respawnEggOnOverworldBedrock();
                }
            }
        }
    }

    public void respawnEggOnOverworldBedrock() {
        long currentTick = Bukkit.getCurrentTick();
        if (currentTick - lastRespawnTick < 2L) {
            return;
        }
        lastRespawnTick = currentTick;

        World overworld = findOverworld();
        if (overworld == null) {
            logger.warning("Cannot respawn Dragon Egg: Overworld (Environment.NORMAL) not found.");
            return;
        }

        Block bedrock = findHighestBedrock(overworld, overworld.getSpawnLocation());
        if (bedrock == null) {
            bedrock = findHighestBedrock(overworld, new Location(overworld, 0, 64, 0));
        }

        Location eggLocation;
        if (bedrock != null) {
            Block above = bedrock.getRelative(BlockFace.UP);
            if (!above.getType().isAir()) {
                above.setType(Material.AIR, false);
            }
            above.setType(Material.DRAGON_EGG, false);
            eggLocation = above.getLocation();
        } else {
            Location spawn = overworld.getSpawnLocation();
            Block baseBlock = overworld.getBlockAt(spawn.getBlockX(), spawn.getBlockY(), spawn.getBlockZ());
            baseBlock.setType(Material.BEDROCK, false);
            Block above = baseBlock.getRelative(BlockFace.UP);
            above.setType(Material.DRAGON_EGG, false);
            eggLocation = above.getLocation();
        }

        Bukkit.broadcastMessage("§5§l[AMCEggVoid] §dЯйцо Дракона упало в пустоту и возродилось на бедроке в Обычном мире §7("
                + eggLocation.getBlockX() + ", " + eggLocation.getBlockY() + ", " + eggLocation.getBlockZ() + ")§d!");
    }

    private World findOverworld() {
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == World.Environment.NORMAL) {
                return world;
            }
        }
        return Bukkit.getWorld("world");
    }

    private Block findHighestBedrock(World world, Location center) {
        int cx = center.getBlockX();
        int cz = center.getBlockZ();
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 2;

        Block best = null;
        double bestDistSq = Double.MAX_VALUE;

        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                int x = cx + dx;
                int z = cz + dz;
                for (int y = maxY; y >= minY; y--) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == Material.BEDROCK) {
                        double distSq = dx * dx + dz * dz;
                        if (best == null || y > best.getY() || (y == best.getY() && distSq < bestDistSq)) {
                            best = block;
                            bestDistSq = distSq;
                        }
                        break;
                    }
                }
            }
        }

        return best;
    }
}
