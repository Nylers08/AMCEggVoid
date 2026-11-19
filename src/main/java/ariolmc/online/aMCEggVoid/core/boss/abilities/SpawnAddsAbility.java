package ariolmc.online.aMCEggVoid.core.boss.abilities;

import ariolmc.online.aMCEggVoid.core.boss.BossAbility;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.persistence.PersistentDataType;

import java.util.Random;

/**
 * Способность босса: призыв зомби-аддов в радиусе от босса.
 */
public class SpawnAddsAbility implements BossAbility {

    private final NamespacedKey addKey;
    private final int cooldownSeconds;
    private final int count;
    private final int minRadius;
    private final int maxRadius;
    private final Random random = new Random();

    public SpawnAddsAbility(NamespacedKey addKey, int cooldownSeconds, int count, int minRadius, int maxRadius) {
        this.addKey = addKey;
        this.cooldownSeconds = cooldownSeconds;
        this.count = count;
        this.minRadius = minRadius;
        this.maxRadius = maxRadius;
    }

    @Override
    public String getId() {
        return "spawn_adds";
    }

    @Override
    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    @Override
    public void execute(LivingEntity boss, World world) {
        Location center = boss.getLocation();
        world.playSound(center, Sound.ENTITY_ZOMBIE_AMBIENT, 1.0f, 0.7f);

        for (int i = 0; i < count; i++) {
            Location spawnLoc = findSafeLocation(center);
            if (spawnLoc == null) {
                continue;
            }
            Zombie add = (Zombie) world.spawnEntity(spawnLoc, EntityType.ZOMBIE);
            add.setShouldBurnInDay(false);
            add.setCustomName("§2Прислужник Бездны");
            add.setCustomNameVisible(true);
            add.getPersistentDataContainer().set(addKey, PersistentDataType.BYTE, (byte) 1);

            world.spawnParticle(Particle.SMOKE, spawnLoc.clone().add(0, 1, 0), 12, 0.3, 0.4, 0.3, 0.02);
        }
    }

    private Location findSafeLocation(Location center) {
        World world = center.getWorld();
        if (world == null) {
            return null;
        }
        int baseY = center.getBlockY();

        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = minRadius + random.nextDouble() * (maxRadius - minRadius);
            int x = (int) Math.round(center.getX() + Math.cos(angle) * dist);
            int z = (int) Math.round(center.getZ() + Math.sin(angle) * dist);
            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }

            int minY = Math.max(world.getMinHeight() + 1, baseY - 4);
            int maxY = Math.min(world.getMaxHeight() - 2, baseY + 4);
            for (int y = maxY; y >= minY; y--) {
                Block ground = world.getBlockAt(x, y - 1, z);
                Block feet = world.getBlockAt(x, y, z);
                Block head = world.getBlockAt(x, y + 1, z);
                if (ground.getType().isSolid() && !ground.isLiquid()
                        && ground.getType() != Material.MAGMA_BLOCK
                        && feet.isPassable() && !feet.isLiquid()
                        && head.isPassable() && !head.isLiquid()) {
                    return new Location(world, x + 0.5, y, z + 0.5, random.nextFloat() * 360f, 0f);
                }
            }
        }
        return null;
    }
}
