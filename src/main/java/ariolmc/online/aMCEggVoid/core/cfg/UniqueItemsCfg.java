package ariolmc.online.aMCEggVoid.core.cfg;

import ariolmc.online.aMCEggVoid.AMCEggVoid;
import ariolmc.online.aMCEggVoid.core.ItemStackBuilder;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UniqueItemsCfg {

    private File file;
    private FileConfiguration config;

    @Getter
    private final Map<String, ItemStack> uniqueItems = new HashMap<>();

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
            boolean unbreakable = itemSec.getBoolean("unbreakable", false);
            Integer customModelData = itemSec.contains("custom-model-data") ? itemSec.getInt("custom-model-data") : null;

            ItemStackBuilder builder = new ItemStackBuilder()
                    .name(name)
                    .amount(1)
                    .materialName(material)
                    .lore(lore)
                    .unbreakable(unbreakable)
                    .customModelData(customModelData);

            // Парсим зачарования
            if (itemSec.isConfigurationSection("enchantments")) {
                ConfigurationSection enchSec = itemSec.getConfigurationSection("enchantments");
                if (enchSec != null) {
                    for (String enchKey : enchSec.getKeys(false)) {
                        Enchantment ench = ItemStackBuilder.matchEnchantment(enchKey);
                        int lvl = enchSec.getInt(enchKey, 1);
                        if (ench != null) {
                            builder.addEnchantment(ench, lvl);
                        }
                    }
                }
            } else if (itemSec.isList("enchantments")) {
                for (String enchEntry : itemSec.getStringList("enchantments")) {
                    String[] parts = enchEntry.split(":");
                    Enchantment ench = ItemStackBuilder.matchEnchantment(parts[0]);
                    int lvl = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                    if (ench != null) {
                        builder.addEnchantment(ench, lvl);
                    }
                }
            }

            // Парсим флаги
            if (itemSec.isList("flags")) {
                List<ItemFlag> flags = new ArrayList<>();
                for (String fName : itemSec.getStringList("flags")) {
                    try {
                        flags.add(ItemFlag.valueOf(fName.toUpperCase()));
                    } catch (IllegalArgumentException ignored) {}
                }
                builder.flags(flags);
            }

            ItemStack item = builder.build();
            this.uniqueItems.put(key.toLowerCase(), item);
        }
    }

    public ItemStack getItem(String id) {
        if (id == null) return null;
        ItemStack item = uniqueItems.get(id.toLowerCase());
        return item != null ? item.clone() : null;
    }
}
