package ariolmc.online.aMCEggVoid.core.boss.abilities;

import ariolmc.online.aMCEggVoid.core.boss.BossAbility;
import ariolmc.online.aMCEggVoid.core.boss.BossManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Способность босса Незера: выбирает случайного атакующего игрока,
 * высоко прыгает в его точку и при приземлении наносит большой урон с частицами огня.
 */
public class LeapSlamAbility implements BossAbility {

    private final Plugin plugin;
    private final BossManager bossManager;
    private final int cooldownSeconds;
    private final double damage;
    private final double slamRadius;
    private final Random random = new Random();

    public LeapSlamAbility(Plugin plugin, BossManager bossManager, int cooldownSeconds, double damage, double slamRadius) {
        this.plugin = plugin;
        this.bossManager = bossManager;
        this.cooldownSeconds = cooldownSeconds;
        this.damage = damage;
        this.slamRadius = slamRadius;
    }

    @Override
    public String getId() {
        return "leap_slam";
    }

    @Override
    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    @Override
    public void execute(LivingEntity boss, World world) {
        Player target = pickTargetPlayer(boss, world);
        if (target == null) {
            return;
        }

        Location targetLoc = target.getLocation().clone();
        Location startLoc = boss.getLocation();

        double dx = targetLoc.getX() - startLoc.getX();
        double dz = targetLoc.getZ() - startLoc.getZ();
        Vector leap = new Vector(dx * 0.11, 1.25, dz * 0.11);
        boss.setVelocity(leap);

        world.playSound(startLoc, Sound.ENTITY_MAGMA_CUBE_JUMP, 1.4f, 0.6f);
        world.spawnParticle(Particle.FLAME, targetLoc.clone().add(0, 0.2, 0), 25, 0.8, 0.15, 0.8, 0.03);

        final int[] ticks = {0};
        final BukkitTask[] taskRef = new BukkitTask[1];
        taskRef[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (boss.isDead() || !boss.isValid()) {
                if (taskRef[0] != null) {
                    taskRef[0].cancel();
                }
                return;
            }

            ticks[0] += 2;
            world.spawnParticle(Particle.FLAME, targetLoc.clone().add(0, 0.2, 0), 10, 0.7, 0.1, 0.7, 0.02);

            boolean landed = ticks[0] >= 18 || (ticks[0] >= 10 && boss.isOnGround());
            if (landed) {
                if (taskRef[0] != null) {
                    taskRef[0].cancel();
                }
                if (boss.getLocation().distanceSquared(targetLoc) > 4.0) {
                    boss.teleport(targetLoc);
                }
                performSlamImpact(boss, world, targetLoc);
            }
        }, 2L, 2L);
    }

    private void performSlamImpact(LivingEntity boss, World world, Location impactLoc) {
        world.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);
        world.spawnParticle(Particle.FLAME, impactLoc.clone().add(0, 0.4, 0), 65, 1.8, 0.5, 1.8, 0.08);
        world.spawnParticle(Particle.LAVA, impactLoc.clone().add(0, 0.4, 0), 20, 1.2, 0.4, 1.2, 0.05);

        double radiusSq = slamRadius * slamRadius;
        for (Player player : world.getPlayers()) {
            if (player.isDead() || player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE) {
                continue;
            }
            if (player.getLocation().distanceSquared(impactLoc) <= radiusSq) {
                player.damage(damage, boss);
                player.setFireTicks(Math.max(player.getFireTicks(), 60));
                Vector push = player.getLocation().toVector().subtract(impactLoc.toVector());
                push.setY(0);
                if (push.lengthSquared() < 0.01) {
                    push = new Vector(0.5, 0, 0.5);
                }
                push.normalize().multiply(0.9).setY(0.45);
                player.setVelocity(player.getVelocity().add(push));
            }
        }
    }

    private Player pickTargetPlayer(LivingEntity boss, World world) {
        List<Player> candidates = new ArrayList<>();
        for (UUID uuid : bossManager.getAttackers()) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null && p.isOnline() && !p.isDead()
                    && p.getWorld().equals(world)
                    && p.getGameMode() != GameMode.SPECTATOR
                    && p.getGameMode() != GameMode.CREATIVE) {
                candidates.add(p);
            }
        }
        if (!candidates.isEmpty()) {
            return candidates.get(random.nextInt(candidates.size()));
        }

        // Запасной вариант: ближайший игрок в радиусе 24 блоков
        Player nearest = null;
        double bestDistSq = 24.0 * 24.0;
        for (Player p : world.getPlayers()) {
            if (!p.isDead() && p.getGameMode() != GameMode.SPECTATOR && p.getGameMode() != GameMode.CREATIVE) {
                double d = p.getLocation().distanceSquared(boss.getLocation());
                if (d < bestDistSq) {
                    bestDistSq = d;
                    nearest = p;
                }
            }
        }
        return nearest;
    }
}
