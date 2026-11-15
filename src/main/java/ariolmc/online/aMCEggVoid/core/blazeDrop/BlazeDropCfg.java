package ariolmc.online.aMCEggVoid.core.blazeDrop;

import ariolmc.online.aMCEggVoid.core.cfg.CfgUtils;
import ariolmc.online.aMCEggVoid.core.cfg.UniqueItemsCfg;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Getter
public class BlazeDropCfg {

    private final UniqueItemsCfg uniqueItemsCfg;
    private final Random random = new Random();

    private boolean enabled = true;
    private double chance = 0.35;
    private boolean playerKillOnly = false;
    private String rollMode = "RANDOM_ONE";
    private final List<BlazeDropItem> items = new ArrayList<>();

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

        double rawChance = section.getDouble("chance", 0.35);
        this.chance = normalizeChance(rawChance);

        loadItems(section);
    }

    private double normalizeChance(double value) {
        if (value > 1.0) {
            return value / 100.0;
        }
        return Math.max(0.0, value);
    }

    private void loadItems(ConfigurationSection section) {
        // Формат 1: items как список (строк или словарей)
        if (section.isList("items")) {
            for (Object obj : section.getList("items")) {
                if (obj instanceof String raw) {
                    BlazeDropItem item = parseStringItem(raw);
                    if (item != null) items.add(item);
                } else if (obj instanceof Map<?, ?> map) {
                    BlazeDropItem item = parseMapItem(map);
                    if (item != null) items.add(item);
                }
            }
            return;
        }

        // Формат 2: items как секция конфигурации
        ConfigurationSection itemsSec = section.getConfigurationSection("items");
        if (itemsSec == null) {
            return;
        }

        for (String key : itemsSec.getKeys(false)) {
            if (itemsSec.isString(key)) {
                String raw = itemsSec.getString(key);
                BlazeDropItem item = parseStringItem(raw);
                if (item != null) items.add(item);
                continue;
            }

            ConfigurationSection itemSec = itemsSec.getConfigurationSection(key);
            if (itemSec != null) {
                String identifier = itemSec.getString("item", itemSec.getString("material", itemSec.getString("id", key)));
                int min = itemSec.getInt("min", 1);
                int max = itemSec.getInt("max", min);
                if (max < min) max = min;
                double itemChance = normalizeChance(itemSec.getDouble("chance", 1.0));
                double weight = itemSec.getDouble("weight", itemChance > 0 ? itemChance : 1.0);

                ItemStack stack = resolveItemStack(identifier);
                if (stack != null) {
                    items.add(new BlazeDropItem(stack, min, max, itemChance, weight));
                }
            }
        }
    }

    private BlazeDropItem parseStringItem(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String[] parts = raw.trim().split(":");
        String id = parts[0].trim();
        int min = 1;
        int max = 1;
        double chance = 1.0;
        double weight = 1.0;

        if (parts.length > 1) {
            try {
                min = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException ignored) {}
        }
        if (parts.length > 2) {
            try {
                max = Integer.parseInt(parts[2].trim());
            } catch (NumberFormatException ignored) {}
        } else {
            max = min;
        }
        if (max < min) max = min;

        if (parts.length > 3) {
            try {
                chance = normalizeChance(Double.parseDouble(parts[3].trim()));
                weight = chance;
            } catch (NumberFormatException ignored) {}
        }

        ItemStack stack = resolveItemStack(id);
        if (stack != null) {
            return new BlazeDropItem(stack, min, max, chance, weight);
        }
        return null;
    }

    private BlazeDropItem parseMapItem(Map<?, ?> map) {
        Object itemObj = map.get("item") != null ? map.get("item") : (map.get("material") != null ? map.get("material") : map.get("id"));
        if (itemObj == null) return null;
        String id = itemObj.toString().trim();

        int min = map.get("min") instanceof Number n ? n.intValue() : 1;
        int max = map.get("max") instanceof Number n ? n.intValue() : min;
        if (max < min) max = min;

        double chance = 1.0;
        if (map.get("chance") instanceof Number n) {
            chance = normalizeChance(n.doubleValue());
        }
        double weight = map.get("weight") instanceof Number n ? n.doubleValue() : (chance > 0 ? chance : 1.0);

        ItemStack stack = resolveItemStack(id);
        if (stack != null) {
            return new BlazeDropItem(stack, min, max, chance, weight);
        }
        return null;
    }

    public ItemStack resolveItemStack(String id) {
        if (id == null || id.isBlank()) return null;
        if (uniqueItemsCfg != null) {
            ItemStack unique = uniqueItemsCfg.getItem(id);
            if (unique != null) {
                return unique.clone();
            }
        }
        Material mat = Material.matchMaterial(id);
        if (mat == null) {
            mat = Material.getMaterial(id.toUpperCase());
        }
        if (mat != null) {
            return new ItemStack(mat, 1);
        }
        return null;
    }

    public List<ItemStack> rollDrops() {
        List<ItemStack> drops = new ArrayList<>();
        if (!enabled || items.isEmpty()) {
            return drops;
        }

        if ("ALL".equalsIgnoreCase(rollMode)) {
            for (BlazeDropItem item : items) {
                double rollChance = item.chance() > 0 ? item.chance() : this.chance;
                if (random.nextDouble() <= rollChance) {
                    drops.addAll(generateStacks(item));
                }
            }
            return drops;
        }

        // По умолчанию режим RANDOM_ONE: сначала проверяем общий шанс
        if (random.nextDouble() > this.chance) {
            return drops;
        }

        BlazeDropItem selected = selectWeightedItem();
        if (selected != null) {
            drops.addAll(generateStacks(selected));
        }
        return drops;
    }

    private BlazeDropItem selectWeightedItem() {
        if (items.isEmpty()) return null;
        double totalWeight = 0.0;
        for (BlazeDropItem item : items) {
            totalWeight += Math.max(0.0001, item.weight());
        }
        double roll = random.nextDouble() * totalWeight;
        double current = 0.0;
        for (BlazeDropItem item : items) {
            current += Math.max(0.0001, item.weight());
            if (roll <= current) {
                return item;
            }
        }
        return items.get(items.size() - 1);
    }

    private List<ItemStack> generateStacks(BlazeDropItem item) {
        List<ItemStack> result = new ArrayList<>();
        int min = item.minAmount();
        int max = Math.max(min, item.maxAmount());
        int amount = min == max ? min : (min + random.nextInt(max - min + 1));
        if (amount <= 0) return result;

        int maxStack = item.itemStack().getMaxStackSize();
        while (amount > 0) {
            ItemStack stack = item.itemStack().clone();
            int count = Math.min(amount, maxStack);
            stack.setAmount(count);
            result.add(stack);
            amount -= count;
        }
        return result;
    }
}
