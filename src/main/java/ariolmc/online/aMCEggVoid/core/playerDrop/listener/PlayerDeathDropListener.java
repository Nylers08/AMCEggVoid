package ariolmc.online.aMCEggVoid.core.playerDrop.listener;

import ariolmc.online.aMCEggVoid.core.EventState;
import ariolmc.online.aMCEggVoid.core.playerDrop.MainMaterialList;
import ariolmc.online.aMCEggVoid.core.playerDrop.PlayerDropCfg;
import ariolmc.online.aMCEggVoid.core.playerDrop.PlayerDropCfg.DeathCauseType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerDeathDropListener implements Listener {

    private record LastAttacker(DeathCauseType cause, long timestampMs) {}

    private final MainMaterialList mainMaterialList;
    private final PlayerDropCfg cfg;
    private final Map<UUID, LastAttacker> recentAttackers = new HashMap<>();

    public PlayerDeathDropListener(MainMaterialList mainMaterialList, PlayerDropCfg cfg) {
        this.mainMaterialList = mainMaterialList;
        this.cfg = cfg;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamagedByEntity(EntityDamageByEntityEvent event) {
        if (!EventState.isRunning()) {
            return;
        }
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        DeathCauseType cause = resolveAttackerCause(event.getDamager(), victim);
        if (cause != DeathCauseType.SELF) {
            recentAttackers.put(victim.getUniqueId(), new LastAttacker(cause, System.currentTimeMillis()));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        recentAttackers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!EventState.isRunning() || event.getKeepInventory()) {
            return;
        }

        Player player = event.getEntity();
        List<ItemStack> drops = event.getDrops();
        List<ItemStack> itemsToKeep = event.getItemsToKeep();

        // Собираем предметы из хотбара (слоты 0..8), чтобы они никогда не выпадали
        List<ItemStack> hotbarItems = collectHotbarItems(player.getInventory());

        Iterator<ItemStack> iterator = drops.iterator();
        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            if (item == null || item.getType().isAir()) {
                continue;
            }
            if (mainMaterialList.contains(item.getType()) || consumeMatchingHotbarItem(hotbarItems, item)) {
                itemsToKeep.add(item);
                iterator.remove();
            }
        }

        if (drops.isEmpty()) {
            recentAttackers.remove(player.getUniqueId());
            return;
        }

        DeathCauseType deathCause = determineDeathCause(player);
        recentAttackers.remove(player.getUniqueId());

        double dropFraction = cfg.rollDropFraction(deathCause);
        int dropCount = (int) Math.round(drops.size() * dropFraction);
        dropCount = Math.max(0, Math.min(drops.size(), dropCount));

        if (dropCount >= drops.size()) {
            return;
        }

        Collections.shuffle(drops);
        List<ItemStack> toDrop = new ArrayList<>(drops.subList(0, dropCount));
        List<ItemStack> toKeepExtra = new ArrayList<>(drops.subList(dropCount, drops.size()));

        drops.clear();
        drops.addAll(toDrop);
        itemsToKeep.addAll(toKeepExtra);
    }

    private List<ItemStack> collectHotbarItems(PlayerInventory inventory) {
        List<ItemStack> hotbar = new ArrayList<>(9);
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack != null && !stack.getType().isAir()) {
                hotbar.add(stack.clone());
            }
        }
        return hotbar;
    }

    private boolean consumeMatchingHotbarItem(List<ItemStack> hotbarItems, ItemStack target) {
        Iterator<ItemStack> it = hotbarItems.iterator();
        while (it.hasNext()) {
            ItemStack hotbarStack = it.next();
            if (hotbarStack.getAmount() == target.getAmount() && hotbarStack.isSimilar(target)) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    private DeathCauseType determineDeathCause(Player player) {
        Player killer = player.getKiller();
        if (killer != null && !killer.getUniqueId().equals(player.getUniqueId())) {
            return DeathCauseType.PLAYER;
        }

        EntityDamageEvent lastDamage = player.getLastDamageCause();
        if (lastDamage instanceof EntityDamageByEntityEvent byEntity) {
            DeathCauseType direct = resolveAttackerCause(byEntity.getDamager(), player);
            if (direct != DeathCauseType.SELF) {
                return direct;
            }
        }

        LastAttacker recent = recentAttackers.get(player.getUniqueId());
        if (recent != null && (System.currentTimeMillis() - recent.timestampMs()) <= cfg.getCombatTagMillis()) {
            return recent.cause();
        }

        return DeathCauseType.SELF;
    }

    private DeathCauseType resolveAttackerCause(Entity damager, Player victim) {
        Entity attacker = damager;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
            attacker = shooter;
        }
        if (attacker instanceof Player p) {
            return p.getUniqueId().equals(victim.getUniqueId()) ? DeathCauseType.SELF : DeathCauseType.PLAYER;
        }
        if (attacker instanceof LivingEntity) {
            return DeathCauseType.MOB;
        }
        return DeathCauseType.SELF;
    }
}
