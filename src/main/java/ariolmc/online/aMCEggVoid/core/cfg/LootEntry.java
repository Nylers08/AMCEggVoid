package ariolmc.online.aMCEggVoid.core.cfg;

import org.bukkit.inventory.ItemStack;

public record LootEntry(
        ItemStack itemStack,
        int minAmount,
        int maxAmount,
        double chance,
        double weight
) {
    public LootEntry(ItemStack itemStack, int minAmount, int maxAmount, double chance) {
        this(itemStack, minAmount, maxAmount, chance, chance > 0 ? chance : 1.0);
    }
}
