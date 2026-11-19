package ariolmc.online.aMCEggVoid.core.boss;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;

/**
 * Заготовка способности босса для дальнейшего расширения.
 */
public class StubBossAbility implements BossAbility {

    private final String id;
    private final int cooldownSeconds;

    public StubBossAbility(String id, int cooldownSeconds) {
        this.id = id.toLowerCase();
        this.cooldownSeconds = cooldownSeconds;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    @Override
    public void execute(LivingEntity boss, World world) {
        // Базовый визуальный/звуковой эффект-заглушка способности
        world.spawnParticle(Particle.SMOKE, boss.getLocation().add(0, 1.2, 0), 18, 0.8, 0.5, 0.8, 0.03);
        world.playSound(boss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 1.2f);
    }
}
