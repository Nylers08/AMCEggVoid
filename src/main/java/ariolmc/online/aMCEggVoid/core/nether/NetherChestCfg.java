package ariolmc.online.aMCEggVoid.core.nether;

import ariolmc.online.aMCEggVoid.core.cfg.CfgUtils;
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
    private final Random random = new Random();

    private boolean enabled = true;
    private int minChests = 3;
    private int maxChests = 8;
    private int minItems = 2;
    private int maxItems = 5;
    private final List<LootEntry> loot = new ArrayList<>();

    public NetherChestCfg(UniqueItemsCfg uniqueItemsCfg) {
        this.uniqueItemsCfg = uniqueItemsCfg;
        load();
    }

    public void load() {
        this.loot.clear();
        FileConfiguration config = CfgUtils.getConfig();

        ConfigurationSection section = config.getConfigurationSection("nether.chests");
        if (section == null) {
            section = config.getConfigurationSection("nether-chests");
        }

        if (section == null) {
            this.enabled = false;
            return;
        }

        this.enabled = section.getBoolean("enabled", true);
        this.minChests = Math.max(0, section.getInt("min-chests", 3));
        this.maxChests = Math.max(this.minChests, section.getInt("max-chests", 8));
        this.minItems = Math.max(0, section.getInt("min-items", 2));
        this.maxItems = Math.max(this.minItems, section.getInt("max-items", 5));

        if (section.contains("loot")) {
            this.loot.addAll(LootConfigHelper.loadLootList(section, "loot", uniqueItemsCfg));
        } else if (section.contains("items")) {
            this.loot.addAll(LootConfigHelper.loadLootList(section, "items", uniqueItemsCfg));
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
