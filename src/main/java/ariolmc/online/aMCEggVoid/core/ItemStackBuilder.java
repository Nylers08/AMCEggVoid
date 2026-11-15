package ariolmc.online.aMCEggVoid.core;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ItemStackBuilder {

    private String name = "";
    private String materialName = "STONE";
    private int amount = 1;
    private List<String> stringLore = new ArrayList<>();
    private final Map<Enchantment, Integer> enchantments = new HashMap<>();
    private boolean unbreakable = false;
    private Integer customModelData = null;
    private final List<ItemFlag> flags = new ArrayList<>();

    public ItemStackBuilder name(String name) {
        this.name = name == null ? "" : name;
        return this;
    }

    public ItemStackBuilder materialName(String materialName) {
        this.materialName = materialName == null ? "STONE" : materialName;
        return this;
    }

    public ItemStackBuilder amount(int amount) {
        if (amount < 1) {
            amount = 1;
        } else if (amount > 64) {
            amount = 64;
        }
        this.amount = amount;
        return this;
    }

    public ItemStackBuilder lore(List<String> lore) {
        this.stringLore = lore == null ? new ArrayList<>() : lore;
        return this;
    }

    public ItemStackBuilder addEnchantment(Enchantment enchantment, int level) {
        if (enchantment != null) {
            this.enchantments.put(enchantment, level);
        }
        return this;
    }

    public ItemStackBuilder enchantments(Map<Enchantment, Integer> enchants) {
        if (enchants != null) {
            this.enchantments.putAll(enchants);
        }
        return this;
    }

    public ItemStackBuilder unbreakable(boolean unbreakable) {
        this.unbreakable = unbreakable;
        return this;
    }

    public ItemStackBuilder customModelData(Integer customModelData) {
        this.customModelData = customModelData;
        return this;
    }

    public ItemStackBuilder flags(List<ItemFlag> flags) {
        if (flags != null) {
            this.flags.addAll(flags);
        }
        return this;
    }

    public ItemStack build() {
        Material material = buildMaterial();
        ItemStack itemStack = new ItemStack(material, amount);
        setMeta(itemStack);

        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            itemStack.addUnsafeEnchantment(entry.getKey(), entry.getValue());
        }

        return itemStack;
    }

    private Material buildMaterial() {
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            material = Material.getMaterial(materialName.toUpperCase());
        }
        if (material == null) {
            material = Material.STONE;
        }
        return material;
    }

    private void setMeta(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;

        setName(meta);
        setLore(meta);

        if (unbreakable) {
            meta.setUnbreakable(true);
        }
        if (customModelData != null) {
            meta.setCustomModelData(customModelData);
        }
        for (ItemFlag flag : flags) {
            meta.addItemFlags(flag);
        }

        itemStack.setItemMeta(meta);
    }

    private void setName(ItemMeta meta) {
        if (!name.isEmpty()) {
            meta.displayName(Component.text(name));
        }
    }

    private void setLore(ItemMeta meta) {
        if (stringLore != null && !stringLore.isEmpty()) {
            List<Component> lore = new ArrayList<>();
            stringLore.forEach(str -> lore.add(Component.text(str)));
            meta.lore(lore);
        }
    }

    public static Enchantment matchEnchantment(String name) {
        if (name == null || name.isBlank()) return null;
        name = name.trim();

        // 1. Попробуем как NamespacedKey (minecraft:sharpness)
        NamespacedKey key = NamespacedKey.fromString(name.contains(":") ? name.toLowerCase() : "minecraft:" + name.toLowerCase());
        if (key != null) {
            Enchantment ench = Registry.ENCHANTMENT.get(key);
            if (ench != null) return ench;
        }

        // 2. Попробуем Enchantment.getByName(SHARPNESS)
        Enchantment ench = Enchantment.getByName(name.toUpperCase());
        if (ench != null) return ench;

        // 3. Попробуем Enchantment.getByKey(NamespacedKey.minecraft(name))
        try {
            return Enchantment.getByKey(NamespacedKey.minecraft(name.toLowerCase()));
        } catch (Exception ignored) {
            return null;
        }
    }
}
