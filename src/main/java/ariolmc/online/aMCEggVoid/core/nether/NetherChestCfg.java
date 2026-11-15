package ariolmc.online.aMCEggVoid.core.nether;

import ariolmc.online.aMCEggVoid.core.cfg.CfgUtils;
import ariolmc.online.aMCEggVoid.core.cfg.LootCfg;
import ariolmc.online.aMCEggVoid.core.cfg.LootConfigHelper;
import ariolmc.online.aMCEggVoid.core.cfg.LootEntry;
import ariolmc.online.aMCEggVoid.core.cfg.UniqueItemsCfg;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Getter
public class NetherChestCfg {

    private final UniqueItemsCfg uniqueItemsCfg;
    private final LootCfg lootCfg;
    private final Random random = new Random();

    private boolean enabled = true;
    private int minChests = 3;
    private int maxChests = 8;
    private int minItems = 2;
    private int maxItems = 5;
    private final List<LootEntry> loot = new ArrayList<>();

    public NetherChestCfg(UniqueItemsCfg uniqueItemsCfg) {
        this(uniqueItemsCfg, null);
    }

    public NetherChestCfg(UniqueItemsCfg uniqueItemsCfg, LootCfg lootCfg) {
        this.uniqueItemsCfg = uniqueItemsCfg;
        this.lootCfg = lootCfg;
        load();
    }

    public void load() {
        this.loot.clear();
        FileConfiguration mainConfig = CfgUtils.getConfig();

        // 1. Настройки генерации (количество сундуков, enabled) берем из главного config.yml
        ConfigurationSection genSection = mainConfig.getConfigurationSection("nether.chests");
        if (genSection == null) {
            genSection = mainConfig.getConfigurationSection("nether-chests");
        }

        if (genSection == null) {
            this.enabled = false;
        } else {
            this.enabled = genSection.getBoolean("enabled", true);
            this.minChests = Math.max(0, genSection.getInt("min-chests", 3));
            this.maxChests = Math.max(this.minChests, genSection.getInt("max-chests", 8));
        }

        // 2. Настройки содержимого сундука (лут, min-items, max-items) берем из loot.yml -> chest.nether
        ConfigurationSection lootSection = null;
        if (lootCfg != null && lootCfg.getConfig() != null) {
            lootSection = lootCfg.getConfig().getConfigurationSection("chest.nether");
        }
        if (lootSection == null) {
            lootSection = genSection;
        }

        if (lootSection != null) {
            this.minItems = Math.max(0, lootSection.getInt("min-items", 2));
            this.maxItems = Math.max(this.minItems, lootSection.getInt("max-items", 5));

            if (lootSection.contains("loot")) {
                this.loot.addAll(LootConfigHelper.loadLootList(lootSection, "loot", uniqueItemsCfg));
            } else if (lootSection.contains("items")) {
                this.loot.addAll(LootConfigHelper.loadLootList(lootSection, "items", uniqueItemsCfg));
            }
        }
    }

    public List<ItemStack> rollChestContents() {
        List<ItemStack> result = new ArrayList<>();
        if (!enabled || loot.isEmpty()) {
            return result;
        }

        int count = minItems == maxItems ? minItems : (minItems + random.nextInt(maxItems - minItems + 1));
        for (int i = 0; i < count; i++) {
            LootEntry selected = LootConfigHelper.selectWeightedItem(loot, random);
            if (selected != null) {
                result.addAll(LootConfigHelper.generateStacks(selected, random));
            }
        }
        return result;
    }
}
