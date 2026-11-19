package ariolmc.online.aMCEggVoid.core.boss.abilities;

import ariolmc.online.aMCEggVoid.core.boss.BossAbility;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Способность босса Незера: волна огня, поджигающая блоки в радиусе от босса.
 */
public class FireWaveAbility implements BossAbility {

    private final Plugin plugin;
    private final int cooldownSeconds;
    private final double maxRadius;

    public FireWaveAbility(Plugin plugin, int cooldownSeconds, double maxRadius) {
        this.plugin = plugin;
        this.cooldownSeconds = cooldownSeconds;
        this.maxRadius = maxRadius;
    }

    @Override
    public String getId() {
        return "fire_wave";
    }

    @Override
    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    @Override
    public void execute(LivingEntity boss, World world) {
        Location origin = boss.getLocation().clone();
        world.playSound(origin, Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.8f);

        final double[] radius = {1.5};
        final BukkitTask[] taskRef = new BukkitTask[1];
        taskRef[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (boss.isDead() || !boss.isValid() || radius[0] > maxRadius) {
                if (taskRef[0] != null) {
                    taskRef[0].cancel();
                }
                return;
            }

            double r = radius[0];
            int points = Math.max(14, (int) (r * 7));
            for (int i = 0; i < points; i++) {
                double angle = (2.0 * Math.PI * i) / points;
                double px = origin.getX() + Math.cos(angle) * r;
                double pz = origin.getZ() + Math.sin(angle) * r;

                Block surface = findIgnitableAirBlock(world, (int) Math.floor(px), origin.getBlockY(), (int) Math.floor(pz));
                if (surface != null) {
                    surface.setType(Material.FIRE, false);
                    world.spawnParticle(Particle.FLAME, surface.getLocation().add(0.5, 0.3, 0.5), 2, 0.15, 0.1, 0.15, 0.02);
                }
            }

            radius[0] += 1.0;
        }, 0L, 2L);
    }

    private Block findIgnitableAirBlock(World world, int x, int baseY, int z) {
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return null;
        }
        int minY = Math.max(world.getMinHeight() + 1, baseY - 3);
        int maxY = Math.min(world.getMaxHeight() - 2, baseY + 2);
        for (int y = maxY; y >= minY; y--) {
            Block ground = world.getBlockAt(x, y - 1, z);
            Block air = world.getBlockAt(x, y, z);
            if (ground.getType().isSolid() && !ground.isLiquid() && air.getType().isAir()) {
                return air;
            }
        }
        return null;
    }
}
