package ariolmc.online.aMCEggVoid.core.trader;

import ariolmc.online.aMCEggVoid.core.cfg.CfgUtils;
import ariolmc.online.aMCEggVoid.core.cfg.LootConfigHelper;
import ariolmc.online.aMCEggVoid.core.cfg.UniqueItemsCfg;
import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
public class TraderCfg {

    public record TradeDefinition(
            ItemStack buy1,
            ItemStack buy2,
            ItemStack sell,
            int maxUses,
            boolean experienceReward
    ) {}

    private final UniqueItemsCfg uniqueItemsCfg;

    private String name = "§6§lТорговец Бездны";
    private Villager.Profession profession = Villager.Profession.CLERIC;
    private boolean invulnerable = true;
    private final List<TradeDefinition> trades = new ArrayList<>();

    public TraderCfg(UniqueItemsCfg uniqueItemsCfg) {
        this.uniqueItemsCfg = uniqueItemsCfg;
        load();
    }

    public void load() {
        this.trades.clear();
        FileConfiguration cfg = CfgUtils.getConfig();
        ConfigurationSection section = cfg.getConfigurationSection("trader");
        if (section == null) {
            return;
        }

        this.name = section.getString("name", "§6§lТорговец Бездны");
        this.invulnerable = section.getBoolean("invulnerable", true);

        String profName = section.getString("profession", "CLERIC");
        this.profession = resolveProfession(profName);

        if (section.isList("trades")) {
            List<?> rawList = section.getList("trades");
            if (rawList != null) {
                for (Object obj : rawList) {
                    if (obj instanceof Map<?, ?> map) {
                        TradeDefinition def = parseTradeMap(map);
                        if (def != null) {
                            this.trades.add(def);
                        }
                    }
                }
            }
        } else if (section.isConfigurationSection("trades")) {
            ConfigurationSection tradesSec = section.getConfigurationSection("trades");
            if (tradesSec != null) {
                for (String key : tradesSec.getKeys(false)) {
                    ConfigurationSection tradeSec = tradesSec.getConfigurationSection(key);
                    if (tradeSec != null) {
                        TradeDefinition def = parseTradeSection(tradeSec);
                        if (def != null) {
                            this.trades.add(def);
                        }
                    }
                }
            }
        }
    }

    private Villager.Profession resolveProfession(String name) {
        if (name != null && !name.isBlank()) {
            NamespacedKey key = NamespacedKey.minecraft(name.trim().toLowerCase());
            Villager.Profession prof = Registry.VILLAGER_PROFESSION.get(key);
            if (prof != null && prof != Villager.Profession.NONE && prof != Villager.Profession.NITWIT) {
                return prof;
            }
        }
        return Villager.Profession.CLERIC;
    }

    private TradeDefinition parseTradeMap(Map<?, ?> map) {
        ItemStack buy1 = parseItemValue(map.get("buy"));
        if (buy1 == null) {
            buy1 = parseItemValue(map.get("buy1"));
        }
        ItemStack buy2 = parseItemValue(map.get("buy2"));
        ItemStack sell = parseItemValue(map.get("sell"));

        if (buy1 == null || sell == null) {
            return null;
        }

        int maxUses = map.get("max-uses") instanceof Number n ? Math.max(1, n.intValue()) : 999;
        boolean expReward = map.get("exp-reward") instanceof Boolean b && b;

        return new TradeDefinition(buy1, buy2, sell, maxUses, expReward);
    }

    private TradeDefinition parseTradeSection(ConfigurationSection sec) {
        ItemStack buy1 = parseConfigItem(sec, "buy");
        if (buy1 == null) {
            buy1 = parseConfigItem(sec, "buy1");
        }
        ItemStack buy2 = parseConfigItem(sec, "buy2");
        ItemStack sell = parseConfigItem(sec, "sell");

        if (buy1 == null || sell == null) {
            return null;
        }

        int maxUses = Math.max(1, sec.getInt("max-uses", 999));
        boolean expReward = sec.getBoolean("exp-reward", false);

        return new TradeDefinition(buy1, buy2, sell, maxUses, expReward);
    }

    private ItemStack parseConfigItem(ConfigurationSection parent, String key) {
        if (!parent.contains(key)) {
            return null;
        }
        if (parent.isString(key)) {
            return parseItemString(parent.getString(key));
        }
        if (parent.isConfigurationSection(key)) {
            ConfigurationSection sub = parent.getConfigurationSection(key);
            if (sub != null) {
                String id = sub.getString("item", sub.getString("material", sub.getString("id")));
                int amount = Math.max(1, sub.getInt("amount", 1));
                return buildSizedItem(id, amount);
            }
        }
        return null;
    }

    private ItemStack parseItemValue(Object val) {
        if (val == null) {
            return null;
        }
        if (val instanceof String str) {
            return parseItemString(str);
        }
        if (val instanceof Map<?, ?> map) {
            Object idObj = map.get("item") != null ? map.get("item") : (map.get("material") != null ? map.get("material") : map.get("id"));
            if (idObj == null) {
                return null;
            }
            int amount = map.get("amount") instanceof Number n ? Math.max(1, n.intValue()) : 1;
            return buildSizedItem(idObj.toString(), amount);
        }
        return null;
    }

    private ItemStack parseItemString(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] parts = raw.trim().split(":");
        String id = parts[0].trim();
        int amount = 1;
        if (parts.length > 1) {
            try {
                amount = Math.max(1, Integer.parseInt(parts[1].trim()));
            } catch (NumberFormatException ignored) {}
        }
        return buildSizedItem(id, amount);
    }

    private ItemStack buildSizedItem(String id, int amount) {
        ItemStack stack = LootConfigHelper.resolveItemStack(id, uniqueItemsCfg);
        if (stack == null) {
            return null;
        }
        stack.setAmount(Math.min(amount, stack.getMaxStackSize()));
        return stack;
    }

    public List<MerchantRecipe> buildRecipes() {
        List<MerchantRecipe> recipes = new ArrayList<>();
        for (TradeDefinition def : trades) {
            MerchantRecipe recipe = new MerchantRecipe(def.sell().clone(), def.maxUses());
            recipe.addIngredient(def.buy1().clone());
            if (def.buy2() != null) {
                recipe.addIngredient(def.buy2().clone());
            }
            recipe.setExperienceReward(def.experienceReward());
            recipe.setVillagerExperience(0);
            recipes.add(recipe);
        }
        return recipes;
    }
}
