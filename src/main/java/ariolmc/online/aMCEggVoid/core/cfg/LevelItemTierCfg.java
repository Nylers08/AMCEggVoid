package ariolmc.online.aMCEggVoid.core.cfg;

import ariolmc.online.aMCEggVoid.core.itemGiver.LevelItemTier;
import ariolmc.online.aMCEggVoid.core.itemGiver.RewardItem;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LevelItemTierCfg {

    private final UniqueItemsCfg uniqueItemsCfg;
    private final LootCfg lootCfg;

    public LevelItemTierCfg(UniqueItemsCfg uniqueItemsCfg) {
        this(uniqueItemsCfg, null);
    }

    public LevelItemTierCfg(UniqueItemsCfg uniqueItemsCfg, LootCfg lootCfg) {
        this.uniqueItemsCfg = uniqueItemsCfg;
        this.lootCfg = lootCfg;
    }

    public List<LevelItemTier> loadLevels() {
        List<LevelItemTier> levelsList = new ArrayList<>();

        FileConfiguration cfg = (lootCfg != null && lootCfg.getConfig() != null && lootCfg.getConfig().isConfigurationSection("item-tier.levels"))
                ? lootCfg.getConfig()
                : CfgUtils.getConfig();

        ConfigurationSection levelsSection = cfg.getConfigurationSection("item-tier.levels");
        if (levelsSection == null) {
            return levelsList;
        }

        for (String key : levelsSection.getKeys(false)) {
            ConfigurationSection levelSection = levelsSection.getConfigurationSection(key);
            if (levelSection == null) {
                continue;
            }

            String name = levelSection.getName();
            int tickStart = levelSection.getInt("tick-start");
            int giveTickDelay = levelSection.getInt("give-tick-delay");
            List<RewardItem> items = loadItems(levelSection);

            LevelItemTier levelItemTier = new LevelItemTier(name, tickStart, giveTickDelay, items);
            levelsList.add(levelItemTier);
        }

        return levelsList;
    }

    private List<RewardItem> loadItems(ConfigurationSection levelSection) {
        List<RewardItem> items = new ArrayList<>();

        // Формат 1: items как список строк или объектов
        // items:
        //   - "mega_diamond:1:12"
        //   - "STICK:1:12"
        // или
        // items:
        //   - item: mega_diamond
        //     min: 1
        //     max: 12
        if (levelSection.isList("items")) {
            for (Object obj : levelSection.getList("items")) {
                if (obj instanceof String raw) {
                    RewardItem item = resolveItem(raw, null, null);
                    if (item != null) {
                        items.add(item);
                    }
                } else if (obj instanceof Map<?, ?> map) {
                    Object itemObj = map.get("item") != null ? map.get("item") : (map.get("material") != null ? map.get("material") : map.get("id"));
                    if (itemObj != null) {
                        String identifier = itemObj.toString();
                        Integer min = map.get("min") instanceof Number n ? n.intValue() : null;
                        Integer max = map.get("max") instanceof Number n ? n.intValue() : null;
                        RewardItem item = resolveItem(identifier, min, max);
                        if (item != null) {
                            items.add(item);
                        }
                    }
                }
            }
            return items;
        }

        // Формат 2: items как секция
        ConfigurationSection itemsSection = levelSection.getConfigurationSection("items");
        if (itemsSection == null) {
            return items;
        }

        for (String key : itemsSection.getKeys(false)) {
            // item1: "mega_diamond:1:12"
            if (itemsSection.isString(key)) {
                String raw = itemsSection.getString(key);
                RewardItem item = resolveItem(raw, null, null);
                if (item != null) {
                    items.add(item);
                }
                continue;
            }

            ConfigurationSection itemSec = itemsSection.getConfigurationSection(key);
            if (itemSec != null) {
                // Идентификатор может быть указан как item, material, id или сам key
                String identifier = itemSec.getString("item", itemSec.getString("material", itemSec.getString("id", key)));
                Integer min = itemSec.contains("min") ? itemSec.getInt("min") : null;
                Integer max = itemSec.contains("max") ? itemSec.getInt("max") : null;

                RewardItem item = resolveItem(identifier, min, max);
                if (item != null) {
                    items.add(item);
                }
            }
        }

        return items;
    }

    /**
     * Преобразует строковый идентификатор (уникальный предмет или ванильный материал) в RewardItem.
     * Кол-во (min, max) настраивается исключительно в главном конфиге config.yml.
     * Поддерживает синтаксис: "IDENTIFIER", "IDENTIFIER:min:max", "IDENTIFIER:amount"
     */
    public RewardItem resolveItem(String rawIdentifier, Integer overrideMin, Integer overrideMax) {
        if (rawIdentifier == null || rawIdentifier.isBlank()) {
            return null;
        }

        String id = rawIdentifier.trim();
        int min = overrideMin != null ? overrideMin : 1;
        int max = overrideMax != null ? overrideMax : 1;

        if (id.contains(":")) {
            String[] parts = id.split(":");
            id = parts[0].trim();
            if (parts.length > 1 && overrideMin == null) {
                try {
                    min = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException ignored) {}
            }
            if (parts.length > 2 && overrideMax == null) {
                try {
                    max = Integer.parseInt(parts[2].trim());
                } catch (NumberFormatException ignored) {}
            } else if (parts.length == 2 && overrideMax == null) {
                max = min;
            }
        }

        if (max < min) {
            max = min;
        }

        // 1. Сначала ищем среди уникальных предметов unique_items.yml
        ItemStack uniqueItem = uniqueItemsCfg.getItem(id);
        if (uniqueItem != null) {
            return new RewardItem(uniqueItem.clone(), min, max);
        }

        // 2. Если не уникальный, проверяем как стандартный Material из Minecraft
        Material mat = Material.matchMaterial(id);
        if (mat == null) {
            mat = Material.getMaterial(id.toUpperCase());
        }
        if (mat != null) {
            return new RewardItem(new ItemStack(mat, 1), min, max);
        }

        return null;
    }
}
