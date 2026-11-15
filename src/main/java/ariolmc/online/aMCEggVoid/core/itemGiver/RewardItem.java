package ariolmc.online.aMCEggVoid.core.itemGiver;

import org.bukkit.inventory.ItemStack;

public record RewardItem(
        ItemStack itemStack,
        int minAmount,
        int maxAmount
) {
}
