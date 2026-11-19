package ariolmc.online.aMCEggVoid.core.mail;

import lombok.Getter;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Почтовый ящик игрока: отдельный список предметов, которые не поместились в инвентарь или были выданы оффлайн.
 */
public class PlayerMail {

    @Getter
    private final String ownerName;
    private final List<ItemStack> items = new ArrayList<>();

    public PlayerMail(String ownerName) {
        this.ownerName = ownerName.toLowerCase();
    }

    public void addItem(ItemStack item) {
        if (item != null && !item.getType().isAir() && item.getAmount() > 0) {
            items.add(item.clone());
        }
    }

    public List<ItemStack> getItems() {
        return Collections.unmodifiableList(items);
    }

    public List<ItemStack> getMutableItems() {
        return items;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int size() {
        return items.size();
    }

    public void clear() {
        items.clear();
    }
}
