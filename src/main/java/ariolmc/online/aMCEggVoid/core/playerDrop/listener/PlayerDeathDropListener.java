package ariolmc.online.aMCEggVoid.core.playerDrop.listener;

import ariolmc.online.aMCEggVoid.core.playerDrop.MainMaterialList;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;
import java.util.List;

public class PlayerDeathDropListener implements Listener {

    private final MainMaterialList mainMaterialList;

    public PlayerDeathDropListener(MainMaterialList mainMaterialList) {
        this.mainMaterialList = mainMaterialList;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        List<ItemStack> drops = event.getDrops();
        List<ItemStack> itemsToKeep = event.getItemsToKeep();

        Iterator<ItemStack> iterator = drops.iterator();
        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            if (item != null && mainMaterialList.contains(item.getType())) {
                itemsToKeep.add(item);
                iterator.remove();
            }
        }
    }
}
