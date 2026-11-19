package ariolmc.online.aMCEggVoid.core.events.impl;

import ariolmc.online.aMCEggVoid.AMCEggVoid;
import ariolmc.online.aMCEggVoid.core.cfg.CfgUtils;
import ariolmc.online.aMCEggVoid.core.cfg.LootConfigHelper;
import ariolmc.online.aMCEggVoid.core.cfg.LootEntry;
import ariolmc.online.aMCEggVoid.core.cfg.UniqueItemsCfg;
import ariolmc.online.aMCEggVoid.core.events.MiniEvent;
import ariolmc.online.aMCEggVoid.core.mail.MailService;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Мини-ивент: выбросить как можно больше предметов из инвентаря в пустоту.
 * Отслеживает выброс предмета игроком и момент попадания предмета в пустоту.
 */
public class VoidDropMiniEvent implements MiniEvent {

    private record TrackedDrop(Item itemEntity, UUID playerUuid, String playerName) {
    }

    private final Plugin plugin;
    private final UniqueItemsCfg uniqueItemsCfg;
    private final Random random = new Random();

    @Getter
    private volatile boolean active = false;

    private final Map<UUID, TrackedDrop> trackedDrops = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> scores = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerNames = new ConcurrentHashMap<>();
    private BukkitTask checkTask;

    public VoidDropMiniEvent(Plugin plugin, UniqueItemsCfg uniqueItemsCfg) {
        this.plugin = plugin;
        this.uniqueItemsCfg = uniqueItemsCfg;
    }

    @Override
    public String getId() {
        return "void_drop";
    }

    @Override
    public String getDisplayName() {
        return "§d§lЖертва Бездне";
    }

    @Override
    public int getDefaultDurationSeconds() {
        return 90;
    }

    @Override
    public void onStart() {
        trackedDrops.clear();
        scores.clear();
        playerNames.clear();
        active = true;

        Bukkit.broadcastMessage("§6AMCEggVoid | §d§lИвент «Жертва Бездне» начался! "
                + "§eВыбрасывайте любые предметы из инвентаря в пустоту — очки начисляются за каждый предмет в стаке. "
                + "§aТоп-3 игрока получат награды!");

        checkTask = Bukkit.getScheduler().runTaskTimer(plugin, this::pollTrackedDrops, 3L, 3L);
    }

    @Override
    public void onStop() {
        if (!active) {
            return;
        }
        active = false;

        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }
        // Досчитываем предметы, которые уже успели упасть ниже границы мира к моменту остановки
        pollTrackedDrops();
        trackedDrops.clear();

        announceAndRewardTop();
        scores.clear();
        playerNames.clear();
    }

    public void trackDrop(Player player, Item itemEntity) {
        if (!active || player == null || itemEntity == null) {
            return;
        }
        trackedDrops.put(itemEntity.getUniqueId(), new TrackedDrop(itemEntity, player.getUniqueId(), player.getName()));
    }

    public boolean isTrackedDrop(UUID itemUuid) {
        return itemUuid != null && trackedDrops.containsKey(itemUuid);
    }

    public void untrackDrop(UUID itemUuid) {
        if (itemUuid != null) {
            trackedDrops.remove(itemUuid);
        }
    }

    private void pollTrackedDrops() {
        if (trackedDrops.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, TrackedDrop> entry : trackedDrops.entrySet()) {
            TrackedDrop drop = entry.getValue();
            Item item = drop.itemEntity();
            int minY = item.getWorld().getMinHeight();
            double y = item.getLocation().getY();

            if (y <= minY - 1.0) {
                awardPoints(drop);
                if (!item.isDead()) {
                    item.remove();
                }
                trackedDrops.remove(entry.getKey());
                continue;
            }

            if (item.isDead() || !item.isValid()) {
                if (y <= minY) {
                    awardPoints(drop);
                }
                trackedDrops.remove(entry.getKey());
            }
        }
    }

    private void awardPoints(TrackedDrop drop) {
        ItemStack stack = drop.itemEntity().getItemStack();
        int amount = stack.getAmount();
        if (amount <= 0) {
            return;
        }
        playerNames.put(drop.playerUuid(), drop.playerName());
        int total = scores.merge(drop.playerUuid(), amount, Integer::sum);

        Player online = Bukkit.getPlayer(drop.playerUuid());
        if (online != null && online.isOnline()) {
            online.sendActionBar(Component.text("§d+" + amount + " §7в пустоту §8| §eВсего очков: §f" + total));
            online.playSound(online.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.3f);
        }
    }

    private void announceAndRewardTop() {
        List<Map.Entry<UUID, Integer>> top = scores.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<UUID, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(3)
                .toList();

        if (top.isEmpty()) {
            Bukkit.broadcastMessage("§6AMCEggVoid | §7Ивент «Жертва Бездне» завершён: никто не выбросил предметы в пустоту.");
            return;
        }

        Bukkit.broadcastMessage("§6AMCEggVoid | §d§lИтоги ивента «Жертва Бездне»:");
        ConfigurationSection rewardsSec = CfgUtils.getConfig()
                .getConfigurationSection("mini-events.events.void_drop.rewards");
        MailService mailService = AMCEggVoid.getMailService();

        for (int i = 0; i < top.size(); i++) {
            int place = i + 1;
            UUID uuid = top.get(i).getKey();
            int score = top.get(i).getValue();
            String name = playerNames.getOrDefault(uuid, "Игрок");

            String medal = switch (place) {
                case 1 -> "§6#1";
                case 2 -> "§7#2";
                default -> "§c#3";
            };
            Bukkit.broadcastMessage("  " + medal + " §f" + name + " §7— §e" + score + " §7предмет(ов)");

            if (mailService != null) {
                List<LootEntry> entries = rewardsSec != null
                        ? LootConfigHelper.loadLootList(rewardsSec, "top-" + place, uniqueItemsCfg)
                        : List.of();
                if (entries.isEmpty()) {
                    entries = getDefaultRewards(place);
                }
                for (LootEntry entry : entries) {
                    if (entry.chance() < 1.0 && random.nextDouble() > entry.chance()) {
                        continue;
                    }
                    for (ItemStack rewardStack : LootConfigHelper.generateStacks(entry, random)) {
                        mailService.deliverOrStore(name, rewardStack);
                    }
                }
            }
        }
    }

    private List<LootEntry> getDefaultRewards(int place) {
        List<String> raw = switch (place) {
            case 1 -> List.of(
                    "void_sword:1",
                    "TOTEM_OF_UNDYING:1",
                    "ENCHANTED_GOLDEN_APPLE:2",
                    "NETHERITE_INGOT:3",
                    "mega_diamond:4",
                    "ENDER_PEARL:16"
            );
            case 2 -> List.of(
                    "void_pickaxe:1",
                    "TOTEM_OF_UNDYING:1",
                    "GOLDEN_APPLE:8",
                    "NETHERITE_INGOT:1",
                    "mega_diamond:2",
                    "BLAZE_ROD:8"
            );
            default -> List.of(
                    "sky_axe:1",
                    "GOLDEN_APPLE:6",
                    "DIAMOND:16",
                    "mega_emerald:2",
                    "OBSIDIAN:16"
            );
        };
        return raw.stream()
                .map(s -> LootConfigHelper.parseStringEntry(s, uniqueItemsCfg))
                .filter(java.util.Objects::nonNull)
                .toList();
    }
}
