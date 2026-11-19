package ariolmc.online.aMCEggVoid.core.boss;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class BossListener implements Listener {

    private final BossManager bossManager;

    public BossListener(BossManager bossManager) {
        this.bossManager = bossManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBossDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof LivingEntity living && bossManager.isEventBoss(living)) {
            if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                event.setCancelled(true);
                bossManager.rescueBossFromVoid(living);
                return;
            }
            bossManager.updateHealthBar(living);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBossDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living) || !bossManager.isEventBoss(living)) {
            return;
        }
        Player attacker = null;
        if (event.getDamager() instanceof Player player) {
            attacker = player;
        } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player shooter) {
            attacker = shooter;
        } else if (event.getDamager() instanceof TNTPrimed tnt && tnt.getSource() instanceof Player source) {
            attacker = source;
        }
        if (attacker != null) {
            bossManager.recordDamage(attacker, event.getFinalDamage());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBossSlimeSplit(SlimeSplitEvent event) {
        if (bossManager.isEventBoss(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBossDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (bossManager.isEventBoss(entity)) {
            event.getDrops().clear();
            bossManager.handleBossDeath(entity);
            return;
        }

        if (bossManager.isBossAdd(entity)) {
            List<ItemStack> addDrops = bossManager.rollAddDrops();
            if (!addDrops.isEmpty()) {
                event.getDrops().clear();
                event.getDrops().addAll(addDrops);
            }
        }
    }
}
