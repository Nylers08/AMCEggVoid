package ariolmc.online.aMCEggVoid.core.events.impl;

import ariolmc.online.aMCEggVoid.core.ItemStackBuilder;
import ariolmc.online.aMCEggVoid.core.MemberList;
import ariolmc.online.aMCEggVoid.core.cfg.CfgUtils;
import ariolmc.online.aMCEggVoid.core.cfg.UniqueItemsCfg;
import ariolmc.online.aMCEggVoid.core.events.MiniEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Мини-ивент «Дождь из Фрикаделек»: с неба над игроками падают предметы «Фрикаделька (халал)».
 */
public class MeatballRainMiniEvent implements MiniEvent {

    private final Plugin plugin;
    private final MemberList memberList;
    private final UniqueItemsCfg uniqueItemsCfg;
    private final Random random = new Random();

    private volatile boolean active = false;
    private BukkitTask spawnTask;

    public MeatballRainMiniEvent(Plugin plugin, MemberList memberList, UniqueItemsCfg uniqueItemsCfg) {
        this.plugin = plugin;
        this.memberList = memberList;
        this.uniqueItemsCfg = uniqueItemsCfg;
    }

    @Override
    public String getId() {
        return "meatball_rain";
    }

    @Override
    public String getDisplayName() {
        return "§6§lДождь из Фрикаделек";
    }

    @Override
    public int getDefaultDurationSeconds() {
        return 45;
    }

    @Override
    public boolean canStart() {
        return !getEligiblePlayers().isEmpty();
    }

    @Override
    public void onStart() {
        active = true;

        FileConfiguration cfg = CfgUtils.getConfig();
        long intervalTicks = Math.max(4L, cfg.getLong("mini-events.events.meatball_rain.spawn-interval-ticks", 10L));
        int perPlayer = Math.max(1, cfg.getInt("mini-events.events.meatball_rain.meatballs-per-player", 2));
        int xzSpread = Math.max(0, cfg.getInt("mini-events.events.meatball_rain.xz-spread", 12));
        int yOffset = Math.max(5, cfg.getInt("mini-events.events.meatball_rain.y-offset", 28));

        Bukkit.broadcastMessage("§6AMCEggVoid | §6§lНачался Дождь из Фрикаделек! §eЛовите с неба халявные §6Фрикадельки (халал)§e!");
        for (Player player : getEligiblePlayers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 0.9f, 1.1f);
        }

        spawnTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!active) {
                return;
            }
            ItemStack meatball = resolveMeatballItem();
            for (Player player : getEligiblePlayers()) {
                for (int i = 0; i < perPlayer; i++) {
                    spawnMeatballAbove(player, meatball, xzSpread, yOffset);
                }
            }
        }, 5L, intervalTicks);
    }

    @Override
    public void onStop() {
        if (!active) {
            return;
        }
        active = false;
        if (spawnTask != null) {
            spawnTask.cancel();
            spawnTask = null;
        }
        Bukkit.broadcastMessage("§6AMCEggVoid | §7Дождь из Фрикаделек закончился.");
    }

    private ItemStack resolveMeatballItem() {
        if (uniqueItemsCfg != null) {
            ItemStack configured = uniqueItemsCfg.getUniqueItems().get("halal_meatball");
            if (configured != null && !configured.getType().isAir()) {
                return configured.clone();
            }
        }
        ItemStack fallback = new ItemStackBuilder()
                .materialName("COOKED_CHICKEN")
                .amount(1)
                .name("§6Фрикаделька (халал)")
                .lore(List.of("§7Сочная фрикаделька прямо с неба", "§a100% халал"))
                .build();
        fallback.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 1);
        return fallback;
    }

    private void spawnMeatballAbove(Player player, ItemStack meatball, int xzSpread, int yOffset) {
        Location base = player.getLocation();
        World world = base.getWorld();
        if (world == null) {
            return;
        }

        int dx = xzSpread > 0 ? (random.nextInt(xzSpread * 2 + 1) - xzSpread) : 0;
        int dz = xzSpread > 0 ? (random.nextInt(xzSpread * 2 + 1) - xzSpread) : 0;
        int targetX = base.getBlockX() + dx;
        int targetZ = base.getBlockZ() + dz;
        int targetY = Math.min(world.getMaxHeight() - 1, base.getBlockY() + yOffset);
        targetY = Math.max(world.getMinHeight() + 1, targetY);

        Location spawnLoc = new Location(world, targetX + 0.5, targetY, targetZ + 0.5);
        if (!world.getWorldBorder().isInside(spawnLoc) || !spawnLoc.getBlock().getType().isAir()) {
            return;
        }

        Item dropped = world.dropItem(spawnLoc, meatball.clone());
        dropped.setPickupDelay(10);
        dropped.setVelocity(new Vector(
                (random.nextDouble() - 0.5) * 0.12,
                -0.15,
                (random.nextDouble() - 0.5) * 0.12
        ));
        world.spawnParticle(Particle.CLOUD, spawnLoc, 2, 0.15, 0.15, 0.15, 0.01);
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
