package ariolmc.online.aMCEggVoid.core.boss.abilities;

import ariolmc.online.aMCEggVoid.core.boss.BossAbility;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Способность босса: ударная волна с частицами блока, по которому проходит волна.
 */
public class ShockwaveAbility implements BossAbility {

    private final Plugin plugin;
    private final int cooldownSeconds;
    private final double maxRadius;
    private final double damage;

    public ShockwaveAbility(Plugin plugin, int cooldownSeconds, double maxRadius, double damage) {
        this.plugin = plugin;
        this.cooldownSeconds = cooldownSeconds;
        this.maxRadius = maxRadius;
        this.damage = damage;
    }

    @Override
    public String getId() {
        return "shockwave";
    }

    @Override
    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    @Override
    public void execute(LivingEntity boss, World world) {
        Location origin = boss.getLocation().clone();
        world.playSound(origin, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 0.8f);

        Set<UUID> hitPlayers = new HashSet<>();
        final double[] radius = {1.2};

        final BukkitTask[] taskRef = new BukkitTask[1];
        taskRef[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (boss.isDead() || !boss.isValid() || radius[0] > maxRadius) {
                if (taskRef[0] != null) {
                    taskRef[0].cancel();
                }
                return;
            }

            double r = radius[0];
            int points = Math.max(16, (int) (r * 8));
            for (int i = 0; i < points; i++) {
                double angle = (2.0 * Math.PI * i) / points;
                double px = origin.getX() + Math.cos(angle) * r;
                double pz = origin.getZ() + Math.sin(angle) * r;

                Block ground = findSurfaceBlock(world, (int) Math.floor(px), origin.getBlockY(), (int) Math.floor(pz));
                if (ground != null) {
                    Location particleLoc = new Location(world, px, ground.getY() + 1.05, pz);
                    world.spawnParticle(Particle.BLOCK, particleLoc, 2, 0.12, 0.08, 0.12, 0.04, ground.getBlockData());
                }
            }

            for (Player player : world.getPlayers()) {
                if (player.isDead() || player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE) {
                    continue;
                }
                if (hitPlayers.contains(player.getUniqueId())) {
                    continue;
                }
                Location pLoc = player.getLocation();
                if (Math.abs(pLoc.getY() - origin.getY()) > 2.8) {
                    continue;
                }
                double dx = pLoc.getX() - origin.getX();
                double dz = pLoc.getZ() - origin.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (Math.abs(dist - r) <= 1.2) {
                    hitPlayers.add(player.getUniqueId());
                    player.damage(damage, boss);
                    Vector knock = new Vector(dx, 0.0, dz);
                    if (knock.lengthSquared() < 0.01) {
                        knock = new Vector(0.5, 0.0, 0.5);
                    }
                    knock.normalize().multiply(0.85).setY(0.35);
                    player.setVelocity(player.getVelocity().add(knock));
                }
            }

            radius[0] += 1.0;
        }, 0L, 2L);
    }

    private Block findSurfaceBlock(World world, int x, int baseY, int z) {
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return null;
        }
        int minY = Math.max(world.getMinHeight(), baseY - 3);
        int maxY = Math.min(world.getMaxHeight() - 2, baseY + 2);
        for (int y = maxY; y >= minY; y--) {
            Block b = world.getBlockAt(x, y, z);
            Block above = world.getBlockAt(x, y + 1, z);
            if (b.getType().isSolid() && !b.isLiquid() && above.isPassable()) {
                return b;
            }
        }
        return null;
    }
}
