package ariolmc.online.aMCEggVoid.core.events.impl;

import ariolmc.online.aMCEggVoid.core.MemberList;
import ariolmc.online.aMCEggVoid.core.cfg.CfgUtils;
import ariolmc.online.aMCEggVoid.core.events.MiniEvent;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Мини-ивент: с неба над игроками падают наковальни (+92 по Y, не выше maxHeight).
 * После приземления или поломки о неполный блок наковальни сразу исчезают без дропа предмета.
 */
public class AnvilRainMiniEvent implements MiniEvent {

    private final Plugin plugin;
    private final MemberList memberList;
    private final Random random = new Random();
    private final Set<UUID> trackedAnvils = ConcurrentHashMap.newKeySet();

    @Getter
    private volatile boolean active = false;
    private BukkitTask spawnTask;

    public AnvilRainMiniEvent(Plugin plugin, MemberList memberList) {
        this.plugin = plugin;
        this.memberList = memberList;
    }

    @Override
    public String getId() {
        return "anvil_rain";
    }

    @Override
    public String getDisplayName() {
        return "§8§lНаковальный Дождь";
    }

    @Override
    public int getDefaultDurationSeconds() {
        return 60;
    }

    @Override
    public boolean canStart() {
        return !getEligiblePlayers().isEmpty();
    }

    @Override
    public void onStart() {
        trackedAnvils.clear();
        active = true;

        FileConfiguration cfg = CfgUtils.getConfig();
        long intervalTicks = Math.max(5L, cfg.getLong("mini-events.events.anvil_rain.spawn-interval-ticks", 15L));
        int anvilsPerPlayer = Math.max(1, cfg.getInt("mini-events.events.anvil_rain.anvils-per-player", 2));
        int xzSpread = Math.max(0, cfg.getInt("mini-events.events.anvil_rain.xz-spread", 10));
        int yOffset = Math.max(1, cfg.getInt("mini-events.events.anvil_rain.y-offset", 92));
        float damagePerBlock = (float) Math.max(0.0, cfg.getDouble("mini-events.events.anvil_rain.damage-per-block", 2.0));
        int maxDamage = Math.max(0, cfg.getInt("mini-events.events.anvil_rain.max-damage", 40));

        Bukkit.broadcastMessage("§6AMCEggVoid | §8§lНаковальный Дождь начался! §eБерегитесь неба — на вас падают наковальни!");

        BlockData anvilData = Material.ANVIL.createBlockData();
        spawnTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!active) {
                return;
            }
            cleanupInvalidAnvils();
            for (Player player : getEligiblePlayers()) {
                for (int i = 0; i < anvilsPerPlayer; i++) {
                    spawnAnvilAbove(player, anvilData, xzSpread, yOffset, damagePerBlock, maxDamage);
                }
            }
        }, 10L, intervalTicks);
    }

    @Override
    public void onStop() {
        active = false;
        if (spawnTask != null) {
            spawnTask.cancel();
            spawnTask = null;
        }
        for (UUID uuid : trackedAnvils) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null && !entity.isDead()) {
                entity.remove();
            }
        }
        trackedAnvils.clear();
    }

    public boolean isTrackedAnvil(UUID uuid) {
        return uuid != null && trackedAnvils.contains(uuid);
    }

    public void untrackAnvil(UUID uuid) {
        if (uuid != null) {
            trackedAnvils.remove(uuid);
        }
    }

    private void spawnAnvilAbove(Player player, BlockData anvilData, int xzSpread, int yOffset,
                                   float damagePerBlock, int maxDamage) {
        Location base = player.getLocation();
        World world = base.getWorld();
        if (world == null) {
            return;
        }

        int dx = xzSpread > 0 ? (random.nextInt(xzSpread * 2 + 1) - xzSpread) : 0;
        int dz = xzSpread > 0 ? (random.nextInt(xzSpread * 2 + 1) - xzSpread) : 0;

        int targetX = base.getBlockX() + dx;
        int targetZ = base.getBlockZ() + dz;
        int maxY = world.getMaxHeight() - 1;
        int targetY = Math.min(maxY, base.getBlockY() + yOffset);
        targetY = Math.max(world.getMinHeight() + 1, targetY);

        Location spawnLoc = new Location(world, targetX + 0.5, targetY, targetZ + 0.5);
        if (!world.getWorldBorder().isInside(spawnLoc)) {
            return;
        }
        if (!spawnLoc.getBlock().getType().isAir()) {
            return;
        }

        FallingBlock fb = world.spawn(spawnLoc, FallingBlock.class, falling -> {
            falling.setBlockData(anvilData);
            falling.setDropItem(false);
            falling.setHurtEntities(true);
            falling.setDamagePerBlock(damagePerBlock);
            falling.setMaxDamage(maxDamage);
        });
        trackedAnvils.add(fb.getUniqueId());
    }

    private void cleanupInvalidAnvils() {
        trackedAnvils.removeIf(uuid -> {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity == null || entity.isDead() || !entity.isValid()) {
                return true;
            }
            if (entity.getLocation().getY() <= entity.getWorld().getMinHeight()) {
                entity.remove();
                return true;
            }
            return false;
        });
    }

    private List<Player> getEligiblePlayers() {
        List<Player> players = new ArrayList<>();
        for (Player p : memberList.buildRecipientsPlayers()) {
            if (p.isOnline() && !p.isDead() && p.getGameMode() != GameMode.SPECTATOR) {
                players.add(p);
            }
        }
        if (players.isEmpty()) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.isOnline() && !p.isDead() && p.getGameMode() != GameMode.SPECTATOR) {
                    players.add(p);
                }
            }
        }
        return players;
    }
}
