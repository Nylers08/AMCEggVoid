package ariolmc.online.aMCEggVoid.core.boss;

import org.bukkit.World;
import org.bukkit.entity.LivingEntity;

/**
 * Контракт способности босса.
 */
public interface BossAbility {

    String getId();

    int getCooldownSeconds();

    void execute(LivingEntity boss, World world);
}
