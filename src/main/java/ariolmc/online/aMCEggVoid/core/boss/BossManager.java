package ariolmc.online.aMCEggVoid.core.boss;

import ariolmc.online.aMCEggVoid.AMCEggVoid;
import ariolmc.online.aMCEggVoid.core.MemberList;
import ariolmc.online.aMCEggVoid.core.boss.abilities.FireWaveAbility;
import ariolmc.online.aMCEggVoid.core.boss.abilities.LeapSlamAbility;
import ariolmc.online.aMCEggVoid.core.boss.abilities.ShockwaveAbility;
import ariolmc.online.aMCEggVoid.core.boss.abilities.SpawnAddsAbility;
import ariolmc.online.aMCEggVoid.core.cfg.LootEntry;
import ariolmc.online.aMCEggVoid.core.cfg.UniqueItemsCfg;
import ariolmc.online.aMCEggVoid.core.mail.MailService;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Управляет спавном босса, полоской здоровья (BossBar), способностями и дропом лута.
 */
public class BossManager {

    private final Plugin plugin;
    private final UniqueItemsCfg uniqueItemsCfg;
    @Getter
    private final BossCfg cfg;
    @Getter
    private final BossSpawnService spawnService;
    private final NamespacedKey bossKey;
    private final NamespacedKey addKey;
    private final Map<String, BossAbility> abilityRegistry = new HashMap<>();
    private final Map<String, Long> abilityLastUsed = new HashMap<>();
    private final Map<UUID, Double> damageByPlayer = new ConcurrentHashMap<>();
    private final Random random = new Random();

    private UUID activeBossUuid;
    private BossDefinition activeBossDef;
    private Location bossHomeLocation;
    private BossBar healthBar;
    private BukkitTask tickTask;

    public BossManager(Plugin plugin, MemberList memberList, UniqueItemsCfg uniqueItemsCfg) {
        this.plugin = plugin;
        this.uniqueItemsCfg = uniqueItemsCfg;
        this.cfg = new BossCfg();
        this.spawnService = new BossSpawnService(memberList, cfg);
        this.bossKey = new NamespacedKey(plugin, "eggvoid_boss_id");
        this.addKey = new NamespacedKey(plugin, "eggvoid_boss_add");

        registerDefaultAbilities();
    }

    private void registerDefaultAbilities() {
        registerAbility(new ShockwaveAbility(plugin, 14, 8.0, 5.0));
        registerAbility(new SpawnAddsAbility(addKey, 20, 3, 3, 7));
        registerAbility(new FireWaveAbility(plugin, 14, 8.0));
        registerAbility(new LeapSlamAbility(plugin, this, 16, 10.0, 3.5));
    }

    public void recordDamage(Player player, double damage) {
        if (player != null && damage > 0.0) {
            damageByPlayer.merge(player.getUniqueId(), damage, Double::sum);
        }
    }

    public Set<UUID> getAttackers() {
        return Collections.unmodifiableSet(damageByPlayer.keySet());
    }

    public void registerAbility(BossAbility ability) {
        if (ability != null) {
            abilityRegistry.put(ability.getId().toLowerCase(), ability);
        }
    }

    public void reloadCfg() {
        cfg.load();
    }

    public boolean canStart() {
        return spawnService.canStartBossEvent();
    }

    public boolean spawnBoss() {
        despawnActiveBoss();

        List<Player> players = spawnService.getEligiblePlayers();
        World targetWorld = spawnService.selectTargetWorld(players);
        if (targetWorld == null) {
            return false;
        }

        BossDefinition def = cfg.getBossFor(targetWorld.getEnvironment());
        if (def == null) {
            return false;
        }

        Location avg = spawnService.computeClampedAveragePoint(targetWorld, players);
        if (avg == null) {
            return false;
        }

        Location spawnLoc = spawnService.findOrBuildSpawnLocation(avg, def.getPlatformRadius());
        if (spawnLoc == null) {
            return false;
        }

        Entity spawned = targetWorld.spawnEntity(spawnLoc, def.getEntityType());
        if (!(spawned instanceof LivingEntity boss)) {
            spawned.remove();
            return false;
        }

        boss.setCustomName(def.getName());
        boss.setCustomNameVisible(true);
        boss.setRemoveWhenFarAway(false);
        boss.setPersistent(true);
        boss.getPersistentDataContainer().set(bossKey, PersistentDataType.STRING, def.getId());

        if (boss instanceof Slime slime) {
            // Максимальный размер магма-куба / слайма (ставить ДО maxHealth, иначе сбросит ХП)
            slime.setSize(4);
        }

        AttributeInstance maxHpAttr = boss.getAttribute(Attribute.MAX_HEALTH);
        if (maxHpAttr != null) {
            maxHpAttr.setBaseValue(def.getMaxHealth());
        }
        boss.setHealth(def.getMaxHealth());

        AttributeInstance scaleAttr = boss.getAttribute(Attribute.SCALE);
        if (scaleAttr != null && def.getScale() != 1.0) {
            scaleAttr.setBaseValue(def.getScale());
        }
        if (boss instanceof Zombie zombie) {
            zombie.setShouldBurnInDay(false);
        }

        this.activeBossUuid = boss.getUniqueId();
        this.activeBossDef = def;
        this.bossHomeLocation = spawnLoc.clone();
        this.abilityLastUsed.clear();
        this.damageByPlayer.clear();

        this.healthBar = Bukkit.createBossBar(def.getName(), def.getBarColor(), BarStyle.SEGMENTED_10);
        updateHealthBar(boss);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.7f, 1.0f);
        }
        Bukkit.broadcastMessage("§6AMCEggVoid | §5§lПоявился босс " + def.getName()
                + " §eна координатах §f" + spawnLoc.getBlockX() + ", " + spawnLoc.getBlockY() + ", " + spawnLoc.getBlockZ() + "§e!");

        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickBoss, 10L, 10L);
        return true;
    }

    private void tickBoss() {
        LivingEntity boss = getActiveBossEntity();
        if (boss == null || boss.isDead()) {
            cleanupBossUi();
            return;
        }

        if (boss.getLocation().getY() <= boss.getWorld().getMinHeight() + 8) {
            rescueBossFromVoid(boss);
        }

        updateHealthBar(boss);
        tickAbilities(boss);
    }

    public void rescueBossFromVoid(LivingEntity boss) {
        if (boss == null || bossHomeLocation == null || activeBossDef == null) {
            return;
        }
        spawnService.ensurePlatform(bossHomeLocation, activeBossDef.getPlatformRadius());
        boss.setFallDistance(0f);
        boss.setVelocity(new Vector(0, 0, 0));
        boss.teleport(bossHomeLocation);

        World world = bossHomeLocation.getWorld();
        if (world != null) {
            Location fxLoc = bossHomeLocation.clone().add(0, 1.0, 0);
            world.spawnParticle(Particle.PORTAL, fxLoc, 45, 0.8, 0.8, 0.8, 0.15);
            world.spawnParticle(Particle.END_ROD, fxLoc, 25, 0.6, 0.8, 0.6, 0.05);
            world.playSound(bossHomeLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 0.8f);
        }
    }

    private void tickAbilities(LivingEntity boss) {
        if (activeBossDef == null) {
            return;
        }
        long now = System.currentTimeMillis();
        for (String abilityId : activeBossDef.getAbilities()) {
            BossAbility ability = abilityRegistry.get(abilityId.toLowerCase());
            if (ability == null) {
                continue;
            }
            long last = abilityLastUsed.getOrDefault(ability.getId(), 0L);
            if (now - last >= ability.getCooldownSeconds() * 1000L) {
                abilityLastUsed.put(ability.getId(), now);
                ability.execute(boss, boss.getWorld());
            }
        }
    }

    public void updateHealthBar(LivingEntity boss) {
        if (healthBar == null || activeBossDef == null || boss == null) {
            return;
        }
        AttributeInstance attr = boss.getAttribute(Attribute.MAX_HEALTH);
        double maxHp = attr != null ? attr.getValue() : activeBossDef.getMaxHealth();
        double hp = Math.max(0.0, boss.getHealth());
        double progress = Math.max(0.0, Math.min(1.0, hp / Math.max(1.0, maxHp)));

        healthBar.setTitle(activeBossDef.getName() + " §7(§c" + (int) Math.ceil(hp) + "§7/§c" + (int) Math.ceil(maxHp) + "❤§7)");
        healthBar.setProgress(progress);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getWorld().equals(boss.getWorld())) {
                if (!healthBar.getPlayers().contains(p)) {
                    healthBar.addPlayer(p);
                }
            } else if (healthBar.getPlayers().contains(p)) {
                healthBar.removePlayer(p);
            }
        }
    }

    public boolean isEventBoss(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        return entity.getPersistentDataContainer().has(bossKey, PersistentDataType.STRING);
    }

    public boolean isBossAdd(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        return entity.getPersistentDataContainer().has(addKey, PersistentDataType.BYTE);
    }

    public List<ItemStack> rollBossDrops() {
        return activeBossDef == null ? List.of() : rollLootList(activeBossDef.getLoot());
    }

    public List<ItemStack> rollAddDrops() {
        return rollLootList(cfg.getAddLoot());
    }

    private List<ItemStack> rollLootList(List<LootEntry> entries) {
        List<ItemStack> drops = new ArrayList<>();
        if (entries == null) {
            return drops;
        }
        for (LootEntry entry : entries) {
            if (random.nextDouble() <= entry.chance() && entry.itemStack() != null) {
                ItemStack item = entry.itemStack().clone();
                int min = Math.max(1, entry.minAmount());
                int max = Math.max(min, entry.maxAmount());
                int amount = min + (max > min ? random.nextInt(max - min + 1) : 0);
                item.setAmount(Math.min(item.getMaxStackSize(), amount));
                drops.add(item);
            }
        }
        return drops;
    }

    public void handleBossDeath(LivingEntity boss) {
        String bossName = activeBossDef != null ? activeBossDef.getName() : "§4§lБосс";
        distributeLootToDamagers();
        cleanupBossUi();
        Bukkit.broadcastMessage("§6AMCEggVoid | §a§lБосс " + bossName + " §aповержен!");
    }

    private void distributeLootToDamagers() {
        if (activeBossDef == null || damageByPlayer.isEmpty()) {
            return;
        }

        List<Map.Entry<UUID, Double>> ranking = new ArrayList<>();
        double totalDamage = 0.0;
        for (Map.Entry<UUID, Double> e : damageByPlayer.entrySet()) {
            if (e.getValue() > 0.0) {
                ranking.add(e);
                totalDamage += e.getValue();
            }
        }
        if (ranking.isEmpty() || totalDamage <= 0.0) {
            return;
        }

        ranking.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // Анонс топа по урону
        Bukkit.broadcastMessage("§6AMCEggVoid | §eТоп урона по боссу:");
        int limit = Math.min(3, ranking.size());
        for (int i = 0; i < limit; i++) {
            Map.Entry<UUID, Double> e = ranking.get(i);
            String name = resolvePlayerName(e.getKey());
            if (name != null) {
                double pct = (e.getValue() / totalDamage) * 100.0;
                Bukkit.broadcastMessage("§e" + (i + 1) + ". §f" + name
                        + " §7— §c" + Math.round(e.getValue()) + " урона §7(§e" + Math.round(pct) + "%§7)");
            }
        }

        // Роллим лут босса и сортируем от самых ценных (редких) к обычным
        List<LootEntry> pool = new ArrayList<>(activeBossDef.getLoot());
        pool.sort(Comparator.comparingDouble(LootEntry::chance));

        List<ItemStack> rolledItems = new ArrayList<>();
        for (LootEntry entry : pool) {
            if (random.nextDouble() <= entry.chance() && entry.itemStack() != null) {
                ItemStack item = entry.itemStack().clone();
                int min = Math.max(1, entry.minAmount());
                int max = Math.max(min, entry.maxAmount());
                int amount = min + (max > min ? random.nextInt(max - min + 1) : 0);
                item.setAmount(Math.min(item.getMaxStackSize(), amount));
                rolledItems.add(item);
            }
        }

        if (rolledItems.isEmpty()) {
            return;
        }

        // Раздаём самые ценные предметы начиная с топ-1 по урону, затем по весу урона
        Map<UUID, Integer> receivedCounts = new HashMap<>();
        int playerCount = ranking.size();

        for (int i = 0; i < rolledItems.size(); i++) {
            ItemStack item = rolledItems.get(i);
            UUID recipientId;
            if (i < playerCount) {
                recipientId = ranking.get(i).getKey();
            } else {
                recipientId = pickWeightedUuidByDamage(ranking, totalDamage);
            }
            if (recipientId != null) {
                giveItemToUuid(recipientId, item);
                receivedCounts.merge(recipientId, 1, Integer::sum);
            }
        }

        for (Map.Entry<UUID, Double> e : ranking) {
            Player p = Bukkit.getPlayer(e.getKey());
            if (p != null && p.isOnline()) {
                int count = receivedCounts.getOrDefault(p.getUniqueId(), 0);
                double pct = (e.getValue() / totalDamage) * 100.0;
                p.sendMessage("§6AMCEggVoid | §aВы внесли §e" + Math.round(pct) + "% §aурона и получили наград: §f" + count + "§a!");
            }
        }
    }

    private UUID pickWeightedUuidByDamage(List<Map.Entry<UUID, Double>> ranking, double totalDamage) {
        double roll = random.nextDouble() * totalDamage;
        double acc = 0.0;
        for (Map.Entry<UUID, Double> e : ranking) {
            acc += e.getValue();
            if (roll <= acc) {
                return e.getKey();
            }
        }
        return ranking.get(0).getKey();
    }

    private String resolvePlayerName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        return offline.getName();
    }

    private void giveItemToUuid(UUID uuid, ItemStack item) {
        MailService mailService = AMCEggVoid.getMailService();
        Player online = Bukkit.getPlayer(uuid);
        if (online != null && online.isOnline()) {
            if (mailService != null) {
                mailService.deliverOrStore(online, item);
            } else {
                Map<Integer, ItemStack> leftover = online.getInventory().addItem(item);
                for (ItemStack extra : leftover.values()) {
                    online.getWorld().dropItemNaturally(online.getLocation(), extra);
                }
            }
            return;
        }
        String name = resolvePlayerName(uuid);
        if (name != null && mailService != null) {
            mailService.deliverOrStore(name, item);
        }
    }

    public void despawnActiveBoss() {
        LivingEntity boss = getActiveBossEntity();
        if (boss != null && !boss.isDead()) {
            boss.remove();
        }
        cleanupBossUi();
    }

    private void cleanupBossUi() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        if (healthBar != null) {
            healthBar.removeAll();
            healthBar = null;
        }
        activeBossUuid = null;
        activeBossDef = null;
        bossHomeLocation = null;
        abilityLastUsed.clear();
        damageByPlayer.clear();
    }

    public LivingEntity getActiveBossEntity() {
        if (activeBossUuid == null) {
            return null;
        }
        Entity entity = Bukkit.getEntity(activeBossUuid);
        return entity instanceof LivingEntity living ? living : null;
    }
}
