package ariolmc.online.aMCEggVoid.core.cfg;

import ariolmc.online.aMCEggVoid.core.itemGiver.LevelItemTier;
import ariolmc.online.aMCEggVoid.core.itemGiver.RewardItem;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

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

        // Формат 1: items как список строк с ID уникальных предметов
        // items:
        //   - "mega_diamond"
        //   - "stick"
        if (levelSection.isList("items")) {
            List<String> idList = levelSection.getStringList("items");
            for (String id : idList) {
                RewardItem base = uniqueItemsCfg.getItem(id);
                if (base != null) {
                    items.add(base);
                }
            }
            return items;
        }

        // Формат 2: items как секция с ключами
        ConfigurationSection itemsSection = levelSection.getConfigurationSection("items");
        if (itemsSection == null) {
            return items;
        }

        for (String key : itemsSection.getKeys(false)) {
            // item1: "mega_diamond"
            if (itemsSection.isString(key)) {
                String id = itemsSection.getString(key);
                RewardItem base = uniqueItemsCfg.getItem(id);
                if (base != null) {
                    items.add(base);
                }
                continue;
            }

            // item1: { id: "mega_diamond", min: 2, max: 5 }
            ConfigurationSection itemSec = itemsSection.getConfigurationSection(key);
            if (itemSec != null) {
                String id = itemSec.getString("id", key);
                RewardItem base = uniqueItemsCfg.getItem(id);
                if (base != null) {
                    int min = itemSec.getInt("min", base.minAmount());
                    int max = itemSec.getInt("max", base.maxAmount());
                    items.add(new RewardItem(base.itemStack(), min, max));
                }
            }
        }

        return items;
    }
}
