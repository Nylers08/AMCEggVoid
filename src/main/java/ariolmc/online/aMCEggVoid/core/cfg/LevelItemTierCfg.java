package ariolmc.online.aMCEggVoid.core.cfg;

import ariolmc.online.aMCEggVoid.core.ItemStackBuilder;
import ariolmc.online.aMCEggVoid.core.itemGiver.LevelItemTier;
import ariolmc.online.aMCEggVoid.core.itemGiver.RewardItem;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class LevelItemTierCfg {

    public List<LevelItemTier> loadLevels(){
        FileConfiguration cfg = CfgUtils.getConfig();

        List<LevelItemTier> levelsList = new ArrayList<>();

        ConfigurationSection levelsSection = cfg.getConfigurationSection("item-tier.levels");
        if (levelsSection == null) {
            return levelsList;
        }
        for (String key : levelsSection.getKeys(false)){
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

    private List<RewardItem> loadItems(ConfigurationSection levelSection){
        List<RewardItem> items = new ArrayList<>();
        ConfigurationSection itemsSection = levelSection.getConfigurationSection("items");
        if (itemsSection == null) {
            return items;
        }

        for (String key : itemsSection.getKeys(false)){
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
            if (itemSection == null) {
                continue;
            }
            ItemStack item = loadItem(itemSection);
            int min = itemSection.getInt("min");
            int max = itemSection.getInt("max");
            RewardItem rewardItem = new RewardItem(item, min, max);
            items.add(rewardItem);
        }

        return items;
    }

    private ItemStack loadItem(ConfigurationSection itemSection){
        String name = itemSection.getString("name", "");
        String materialName = itemSection.getString("material", "STONE");
        List<String> lore = itemSection.getStringList("lore");

        return new ItemStackBuilder()
                .name(name)
                .amount(1)
                .materialName(materialName)
                .lore(lore)
                .build();
    }
}
