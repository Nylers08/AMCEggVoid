package ariolmc.online.aMCEggVoid.core.blazeDrop;

import org.bukkit.inventory.ItemStack;

public record BlazeDropItem(
        ItemStack itemStack,
        int minAmount,
        int maxAmount,
        double chance,
        double weight
) {
    public BlazeDropItem(ItemStack itemStack, int minAmount, int maxAmount, double chance) {
        this(itemStack, minAmount, maxAmount, chance, chance > 0 ? chance : 1.0);
    }
}
