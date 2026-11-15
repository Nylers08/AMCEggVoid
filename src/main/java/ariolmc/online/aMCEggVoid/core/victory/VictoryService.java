package ariolmc.online.aMCEggVoid.core.victory;

import ariolmc.online.aMCEggVoid.AMCEggVoid;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Управляет определением победителя, генерацией победной платформы с пьедесталом
 * в Верхнем мире, телепортацией игроков и праздничным салютом.
 */
public class VictoryService {

    private static final int PLATFORM_RADIUS = 10;
    private static final int PLATFORM_Y = 180;
    private static final int CELEBRATION_TICKS = 300; // 15 секунд

    private final Plugin plugin;
    private boolean victoryTriggered = false;
    private BukkitTask celebrationTask;
    private BossBar victoryBossBar;
    private ArmorStand winnerHologram;

    public VictoryService(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean isVictoryTriggered() {
        return victoryTriggered;
    }

    public void reset() {
        victoryTriggered = false;
        stopCelebration();
    }

    public void stopCelebration() {
        if (celebrationTask != null) {
            celebrationTask.cancel();
            celebrationTask = null;
        }
        if (victoryBossBar != null) {
            victoryBossBar.removeAll();
            victoryBossBar = null;
        }
        if (winnerHologram != null && !winnerHologram.isDead()) {
            winnerHologram.remove();
            winnerHologram = null;
        }
    }

    public void triggerVictory(Player winner) {
        if (victoryTriggered) {
            return;
        }
        victoryTriggered = true;

        // Останавливаем цикл выдачи предметов и переводим EventState в stop
        if (AMCEggVoid.getItemGiverModule() != null) {
            AMCEggVoid.getItemGiverModule().getItemGiver().stopGiveCycle();
        }

        World overworld = findOverworld();
        if (overworld == null) {
            Bukkit.broadcastMessage("§6§l[AMCEggVoid] §eПобедитель: §a" + winner.getName() + "§e!");
            return;
        }

        buildVictoryPlatform(overworld);
        teleportPlayers(overworld, winner);
        announceVictory(winner);
        startCelebrationEffects(overworld, winner);
    }

    private World findOverworld() {
        for (World w : Bukkit.getWorlds()) {
            if (w.getEnvironment() == World.Environment.NORMAL) {
                return w;
            }
        }
        return Bukkit.getWorld("world");
    }

    private void buildVictoryPlatform(World world) {
        // Очищаем пространство над платформой
        for (int x = -PLATFORM_RADIUS - 1; x <= PLATFORM_RADIUS + 1; x++) {
            for (int z = -PLATFORM_RADIUS - 1; z <= PLATFORM_RADIUS + 1; z++) {
                if (x * x + z * z <= (PLATFORM_RADIUS + 1) * (PLATFORM_RADIUS + 1)) {
                    for (int y = PLATFORM_Y + 1; y <= PLATFORM_Y + 14; y++) {
                        world.getBlockAt(x, y, z).setType(Material.AIR, false);
                    }
                }
            }
        }

        // Основание круглой платформы
        for (int x = -PLATFORM_RADIUS; x <= PLATFORM_RADIUS; x++) {
            for (int z = -PLATFORM_RADIUS; z <= PLATFORM_RADIUS; z++) {
                double distSq = x * x + z * z;
                if (distSq <= PLATFORM_RADIUS * PLATFORM_RADIUS + 0.5) {
                    Material mat;
                    if (distSq >= (PLATFORM_RADIUS - 1.2) * (PLATFORM_RADIUS - 1.2)) {
                        mat = ((Math.abs(x) <= 1 || Math.abs(z) <= 1) ? Material.SEA_LANTERN : Material.GOLD_BLOCK);
                    } else if (distSq <= 16.5) {
                        mat = Material.PURPUR_BLOCK;
                    } else if ((Math.abs(x) + Math.abs(z)) % 2 == 0) {
                        mat = Material.SMOOTH_QUARTZ;
                    } else {
                        mat = Material.QUARTZ_BLOCK;
                    }
                    world.getBlockAt(x, PLATFORM_Y, z).setType(mat, false);
                }
            }
        }

        // Пьедестал в центре (0, PLATFORM_Y, 0)
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Material stepMat = (x == 0 && z == 0) ? Material.GOLD_BLOCK : Material.PURPUR_PILLAR;
                world.getBlockAt(x, PLATFORM_Y + 1, z).setType(stepMat, false);
            }
        }
        world.getBlockAt(0, PLATFORM_Y + 2, 0).setType(Material.DIAMOND_BLOCK, false);

        // 4 декоративные колонны с фонарями по углам вокруг пьедестала
        int[][] corners = {{-4, -4}, {-4, 4}, {4, -4}, {4, 4}};
        for (int[] c : corners) {
            world.getBlockAt(c[0], PLATFORM_Y + 1, c[1]).setType(Material.QUARTZ_PILLAR, false);
            world.getBlockAt(c[0], PLATFORM_Y + 2, c[1]).setType(Material.QUARTZ_PILLAR, false);
            world.getBlockAt(c[0], PLATFORM_Y + 3, c[1]).setType(Material.END_ROD, false);
        }

        // Голограмма над пьедесталом
        if (winnerHologram != null && !winnerHologram.isDead()) {
            winnerHologram.remove();
        }
    }

    private void teleportPlayers(World overworld, Player winner) {
        Location pedestalTop = new Location(overworld, 0.5, PLATFORM_Y + 3.0, 0.5, 0f, 0f);

        // Голограмма над головой победителя
        winnerHologram = (ArmorStand) overworld.spawnEntity(
                new Location(overworld, 0.5, PLATFORM_Y + 5.3, 0.5), EntityType.ARMOR_STAND);
        winnerHologram.setVisible(false);
        winnerHologram.setGravity(false);
        winnerHologram.setMarker(true);
        winnerHologram.setCustomName("§6§l★ ПОБЕДИТЕЛЬ: §e§l" + winner.getName() + " §6§l★");
        winnerHologram.setCustomNameVisible(true);

        winner.setFallDistance(0f);
        winner.setFireTicks(0);
        winner.teleport(pedestalTop);
        winner.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, CELEBRATION_TICKS + 100, 0, false, false));

        List<Player> spectators = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getUniqueId().equals(winner.getUniqueId())) {
                spectators.add(p);
            }
        }

        int count = spectators.size();
        double ringRadius = 6.5;
        for (int i = 0; i < count; i++) {
            Player p = spectators.get(i);
            double angle = 2.0 * Math.PI * i / Math.max(1, count);
            double sx = 0.5 + ringRadius * Math.cos(angle);
            double sz = 0.5 + ringRadius * Math.sin(angle);
            Location loc = new Location(overworld, sx, PLATFORM_Y + 1.0, sz);
            loc.setDirection(pedestalTop.toVector().subtract(loc.toVector()));

            p.setFallDistance(0f);
            p.setFireTicks(0);
            p.teleport(loc);
        }
    }

    private void announceVictory(Player winner) {
        Bukkit.broadcastMessage(" ");
        Bukkit.broadcastMessage("§5§l✦══════════════════════════════════════✦");
        Bukkit.broadcastMessage("§6§l          ИВЕНТ ЗАВЕРШЁН — ПОБЕДА!");
        Bukkit.broadcastMessage("§f   Игрок §e§l" + winner.getName() + " §fподобрал §d§lЯйцо Дракона§f!");
        Bukkit.broadcastMessage("§5§l✦══════════════════════════════════════✦");
        Bukkit.broadcastMessage(" ");

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle("§6§lПОБЕДИТЕЛЬ: §e" + winner.getName(), "§dЯйцо Дракона захвачено!", 10, 100, 20);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }

        if (victoryBossBar != null) {
            victoryBossBar.removeAll();
        }
        victoryBossBar = Bukkit.createBossBar(
                "§6§l★ ПОБЕДИТЕЛЬ ИВЕНТА: §e§l" + winner.getName() + " §6§l★",
                BarColor.YELLOW,
                BarStyle.SOLID
        );
        for (Player p : Bukkit.getOnlinePlayers()) {
            victoryBossBar.addPlayer(p);
        }
    }

    private void startCelebrationEffects(World overworld, Player winner) {
        if (celebrationTask != null) {
            celebrationTask.cancel();
        }

        final int[] elapsed = {0};
        celebrationTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (elapsed[0] >= CELEBRATION_TICKS) {
                stopCelebration();
                return;
            }

            // Салюты вокруг платформы
            spawnRandomFirework(overworld);
            if (elapsed[0] % 20 == 0) {
                spawnRandomFirework(overworld);
            }

            // Частицы вокруг победителя на пьедестале
            if (winner.isOnline()) {
                overworld.spawnParticle(Particle.TOTEM_OF_UNDYING,
                        new Location(overworld, 0.5, PLATFORM_Y + 4.0, 0.5),
                        18, 0.6, 0.8, 0.6, 0.15);
            }

            elapsed[0] += 10;
        }, 5L, 10L);
    }

    private void spawnRandomFirework(World world) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double angle = rng.nextDouble(0, Math.PI * 2);
        double dist = rng.nextDouble(3.5, PLATFORM_RADIUS - 0.5);
        double x = 0.5 + dist * Math.cos(angle);
        double z = 0.5 + dist * Math.sin(angle);

        Location fwLoc = new Location(world, x, PLATFORM_Y + 1.5, z);
        Firework fw = (Firework) world.spawnEntity(fwLoc, EntityType.FIREWORK_ROCKET);
        FireworkMeta meta = fw.getFireworkMeta();

        Color[] palette = {Color.PURPLE, Color.FUCHSIA, Color.YELLOW, Color.ORANGE, Color.AQUA, Color.WHITE};
        Color c1 = palette[rng.nextInt(palette.length)];
        Color c2 = palette[rng.nextInt(palette.length)];
        FireworkEffect.Type[] types = {
                FireworkEffect.Type.BALL_LARGE,
                FireworkEffect.Type.STAR,
                FireworkEffect.Type.BURST
        };
        FireworkEffect effect = FireworkEffect.builder()
                .with(types[rng.nextInt(types.length)])
                .withColor(c1)
                .withFade(c2)
                .flicker(true)
                .trail(true)
                .build();

        meta.addEffect(effect);
        meta.setPower(rng.nextInt(1, 3));
        fw.setFireworkMeta(meta);
    }
}
