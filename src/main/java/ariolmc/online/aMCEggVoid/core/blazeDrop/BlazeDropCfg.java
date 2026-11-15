package ariolmc.online.aMCEggVoid.core.blazeDrop;

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
public class BlazeDropCfg {

    private final UniqueItemsCfg uniqueItemsCfg;
    private final Random random = new Random();

    private boolean enabled = true;
    private double chance = 0.35;
    private boolean playerKillOnly = false;
    private String rollMode = "RANDOM_ONE";
    private final List<LootEntry> items = new ArrayList<>();

    public BlazeDropCfg(UniqueItemsCfg uniqueItemsCfg) {
        this.uniqueItemsCfg = uniqueItemsCfg;
        load();
    }

    public void load() {
        this.items.clear();
        FileConfiguration config = CfgUtils.getConfig();

        ConfigurationSection section = config.getConfigurationSection("blaze-drop");
        if (section == null) {
            section = config.getConfigurationSection("nether.blaze-drop");
        }

        if (section == null) {
            this.enabled = false;
            return;
        }

        this.enabled = section.getBoolean("enabled", true);
        this.playerKillOnly = section.getBoolean("player-kill-only", false);
        this.rollMode = section.getString("roll-mode", "RANDOM_ONE").toUpperCase();
        this.chance = LootConfigHelper.normalizeChance(section.getDouble("chance", 0.35));

        this.items.addAll(LootConfigHelper.loadLootList(section, "items", uniqueItemsCfg));
    }

    public List<ItemStack> rollDrops() {
        List<ItemStack> drops = new ArrayList<>();
        if (!enabled || items.isEmpty()) {
            return drops;
        }

        if ("ALL".equalsIgnoreCase(rollMode)) {
            for (LootEntry item : items) {
                double rollChance = item.chance() > 0 ? item.chance() : this.chance;
                if (random.nextDouble() <= rollChance) {
                    drops.addAll(LootConfigHelper.generateStacks(item, random));
                }
            }
            return drops;
        }

        if (random.nextDouble() > this.chance) {
            return drops;
        }

        LootEntry selected = LootConfigHelper.selectWeightedItem(items, random);
        if (selected != null) {
            drops.addAll(LootConfigHelper.generateStacks(selected, random));
        }
        return drops;
    }
}
