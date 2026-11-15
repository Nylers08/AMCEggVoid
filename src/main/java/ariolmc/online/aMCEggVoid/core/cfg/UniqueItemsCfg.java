package ariolmc.online.aMCEggVoid.core.cfg;

import ariolmc.online.aMCEggVoid.AMCEggVoid;
import ariolmc.online.aMCEggVoid.core.ItemStackBuilder;
import ariolmc.online.aMCEggVoid.core.itemGiver.RewardItem;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UniqueItemsCfg {

    private File file;
    private FileConfiguration config;

    @Getter
    private final Map<String, RewardItem> uniqueItems = new HashMap<>();

    public UniqueItemsCfg() {
        load();
    }

    public void load() {
        AMCEggVoid plugin = AMCEggVoid.getInstance();
        if (plugin != null) {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            this.file = new File(plugin.getDataFolder(), "unique_items.yml");
            if (!this.file.exists()) {
                plugin.saveResource("unique_items.yml", false);
            }

            this.config = YamlConfiguration.loadConfiguration(this.file);

            InputStream defConfigStream = plugin.getResource("unique_items.yml");
            if (defConfigStream != null) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));
                this.config.setDefaults(defConfig);
            }
        } else {
            this.config = new YamlConfiguration();
        }

        parseItems();
    }

    private void parseItems() {
        this.uniqueItems.clear();

        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) {
            itemsSection = config;
        }

        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection itemSec = itemsSection.getConfigurationSection(key);
            if (itemSec == null) {
                continue;
            }

            String name = itemSec.getString("name", "");
            String material = itemSec.getString("material", "STONE");
            List<String> lore = itemSec.getStringList("lore");
            int min = itemSec.getInt("min", 1);
            int max = itemSec.getInt("max", 1);
            if (max < min) {
                max = min;
            }

            ItemStack item = new ItemStackBuilder()
                    .name(name)
                    .amount(1)
                    .materialName(material)
                    .lore(lore)
                    .build();

            RewardItem rewardItem = new RewardItem(item, min, max);
            this.uniqueItems.put(key.toLowerCase(), rewardItem);
        }
    }

    public RewardItem getItem(String id) {
        if (id == null) return null;
        return uniqueItems.get(id.toLowerCase());
    }
}
