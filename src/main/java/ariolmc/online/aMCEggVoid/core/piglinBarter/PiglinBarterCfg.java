package ariolmc.online.aMCEggVoid.core.piglinBarter;

import ariolmc.online.aMCEggVoid.core.cfg.LootCfg;
import ariolmc.online.aMCEggVoid.core.cfg.LootConfigHelper;
import ariolmc.online.aMCEggVoid.core.cfg.LootEntry;
import ariolmc.online.aMCEggVoid.core.cfg.UniqueItemsCfg;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Getter
public class PiglinBarterCfg {

    private final UniqueItemsCfg uniqueItemsCfg;
    private final LootCfg lootCfg;
    private final Random random = new Random();

    private boolean enabled = true;
    private boolean replaceVanilla = true;
    private String rollMode = "ONE";
    private final List<LootEntry> items = new ArrayList<>();

    public PiglinBarterCfg(UniqueItemsCfg uniqueItemsCfg, LootCfg lootCfg) {
        this.uniqueItemsCfg = uniqueItemsCfg;
        this.lootCfg = lootCfg;
        load();
    }

    public void load() {
        this.items.clear();

        if (lootCfg == null || lootCfg.getConfig() == null) {
            this.enabled = false;
            return;
        }

        ConfigurationSection section = lootCfg.getConfig().getConfigurationSection("piglin-barter");
        if (section == null) {
            this.enabled = false;
            return;
        }

        this.enabled = section.getBoolean("enabled", true);
        this.replaceVanilla = section.getBoolean("replace-vanilla", true);
        this.rollMode = section.getString("roll-mode", "ONE").toUpperCase();

        this.items.addAll(LootConfigHelper.loadLootList(section, "items", uniqueItemsCfg));
    }

    public List<ItemStack> rollBarterDrops() {
        List<ItemStack> drops = new ArrayList<>();
        if (!enabled || items.isEmpty()) {
            return drops;
        }

        List<LootEntry> candidates = new ArrayList<>(items);
        Collections.shuffle(candidates, random);

        for (LootEntry entry : candidates) {
            if (random.nextDouble() <= entry.chance()) {
                drops.addAll(LootConfigHelper.generateStacks(entry, random));
                if (!"ALL".equalsIgnoreCase(rollMode)) {
                    break;
                }
            }
        }

        return drops;
    }
}
