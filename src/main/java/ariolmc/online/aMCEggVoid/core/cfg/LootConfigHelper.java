package ariolmc.online.aMCEggVoid.core.cfg;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class LootConfigHelper {

    private LootConfigHelper() {}

    public static double normalizeChance(double value) {
        if (value > 1.0) {
            return value / 100.0;
        }
        return Math.max(0.0, value);
    }

    public static List<LootEntry> loadLootList(ConfigurationSection section, String key, UniqueItemsCfg uniqueItemsCfg) {
        List<LootEntry> result = new ArrayList<>();
        if (section == null) return result;

        if (section.isList(key)) {
            List<?> list = section.getList(key);
            if (list != null) {
                for (Object obj : list) {
                    if (obj instanceof String raw) {
                        LootEntry entry = parseStringEntry(raw, uniqueItemsCfg);
                        if (entry != null) result.add(entry);
                    } else if (obj instanceof Map<?, ?> map) {
                        LootEntry entry = parseMapEntry(map, uniqueItemsCfg);
                        if (entry != null) result.add(entry);
                    }
                }
            }
            return result;
        }

        ConfigurationSection itemsSec = section.getConfigurationSection(key);
        if (itemsSec == null) return result;

        for (String itemKey : itemsSec.getKeys(false)) {
            if (itemsSec.isString(itemKey)) {
                LootEntry entry = parseStringEntry(itemsSec.getString(itemKey), uniqueItemsCfg);
                if (entry != null) result.add(entry);
                continue;
            }

            ConfigurationSection itemSec = itemsSec.getConfigurationSection(itemKey);
            if (itemSec != null) {
                String identifier = itemSec.getString("item", itemSec.getString("material", itemSec.getString("id", itemKey)));
                int min = itemSec.getInt("min", 1);
                int max = itemSec.getInt("max", min);
                if (max < min) max = min;
                double chance = normalizeChance(itemSec.getDouble("chance", 1.0));
                double weight = itemSec.getDouble("weight", chance > 0 ? chance : 1.0);

                ItemStack stack = resolveItemStack(identifier, uniqueItemsCfg);
                if (stack != null) {
                    result.add(new LootEntry(stack, min, max, chance, weight));
                }
            }
        }
        return result;
    }

    public static LootEntry parseStringEntry(String raw, UniqueItemsCfg uniqueItemsCfg) {
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

        ItemStack stack = resolveItemStack(id, uniqueItemsCfg);
        if (stack != null) {
            return new LootEntry(stack, min, max, chance, weight);
        }
        return null;
    }

    public static LootEntry parseMapEntry(Map<?, ?> map, UniqueItemsCfg uniqueItemsCfg) {
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

        ItemStack stack = resolveItemStack(id, uniqueItemsCfg);
        if (stack != null) {
            return new LootEntry(stack, min, max, chance, weight);
        }
        return null;
    }

    public static ItemStack resolveItemStack(String id, UniqueItemsCfg uniqueItemsCfg) {
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

        String upper = id.trim().toUpperCase();
        if (upper.startsWith("SPLASH_POTION_")) {
            return createPotion(Material.SPLASH_POTION, upper.substring("SPLASH_POTION_".length()));
        }
        if (upper.startsWith("POTION_")) {
            return createPotion(Material.POTION, upper.substring("POTION_".length()));
        }
        return null;
    }

    private static ItemStack createPotion(Material potionMat, String typeName) {
        try {
            PotionType type = PotionType.valueOf(typeName);
            ItemStack item = new ItemStack(potionMat, 1);
            if (item.getItemMeta() instanceof PotionMeta meta) {
                meta.setBasePotionType(type);
                item.setItemMeta(meta);
            }
            return item;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static LootEntry selectWeightedItem(List<LootEntry> items, Random random) {
        if (items == null || items.isEmpty()) return null;
        double totalWeight = 0.0;
        for (LootEntry item : items) {
            totalWeight += Math.max(0.0001, item.weight());
        }
        double roll = random.nextDouble() * totalWeight;
        double current = 0.0;
        for (LootEntry item : items) {
            current += Math.max(0.0001, item.weight());
            if (roll <= current) {
                return item;
            }
        }
        return items.get(items.size() - 1);
    }

    public static List<ItemStack> generateStacks(LootEntry item, Random random) {
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
