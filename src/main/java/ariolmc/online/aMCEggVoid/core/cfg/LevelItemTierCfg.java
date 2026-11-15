package ariolmc.online.aMCEggVoid.core.cfg;

import ariolmc.online.aMCEggVoid.core.itemGiver.LevelItemTier;
import ariolmc.online.aMCEggVoid.core.itemGiver.RewardItem;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class LevelItemTierCfg {

    private final UniqueItemsCfg uniqueItemsCfg;

    public LevelItemTierCfg(UniqueItemsCfg uniqueItemsCfg) {
        this.uniqueItemsCfg = uniqueItemsCfg;
    }

    public List<LevelItemTier> loadLevels() {
        FileConfiguration cfg = CfgUtils.getConfig();
        List<LevelItemTier> levelsList = new ArrayList<>();

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

        // Формат 1: items как список строк
        // items:
        //   - "mega_diamond"
        //   - "DIAMOND:1:12"
        //   - "STICK"
        if (levelSection.isList("items")) {
            List<String> idList = levelSection.getStringList("items");
            for (String raw : idList) {
                RewardItem item = resolveItem(raw, null, null);
                if (item != null) {
                    items.add(item);
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
            // item1: "mega_diamond" или item1: "DIAMOND:1:10"
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

        // 1. Сначала ищем среди уникальных предметов unique_items.yml
        RewardItem unique = uniqueItemsCfg.getItem(id);
        if (unique != null) {
            int finalMin = overrideMin != null ? overrideMin : (rawIdentifier.contains(":") ? min : unique.minAmount());
            int finalMax = overrideMax != null ? overrideMax : (rawIdentifier.contains(":") ? max : unique.maxAmount());
            if (finalMax < finalMin) finalMax = finalMin;
            return new RewardItem(unique.itemStack().clone(), finalMin, finalMax);
        }

        // 2. Если не уникальный, проверяем как стандартный Material из Minecraft
        Material mat = Material.matchMaterial(id);
        if (mat == null) {
            mat = Material.getMaterial(id.toUpperCase());
        }
        if (mat != null) {
            if (max < min) max = min;
            return new RewardItem(new ItemStack(mat, 1), min, max);
        }

        return null;
    }
}
