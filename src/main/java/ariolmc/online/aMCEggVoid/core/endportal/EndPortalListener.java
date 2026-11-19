package ariolmc.online.aMCEggVoid.core.endportal;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class EndPortalListener implements Listener {

    private final EndPortalAltar altar;

    public EndPortalListener(EndPortalAltar altar) {
        this.altar = altar;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || !altar.isFrameBlock(block)) {
            return;
        }

        // Полностью запрещаем ручную вставку Ока Эндера
        event.setCancelled(true);

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.ENDER_EYE) {
            player.sendMessage("§6AMCEggVoid | §cНельзя вставлять Око Эндера вручную! Бросайте нужные ресурсы внутрь портала.");
        } else if (altar.getFilledEyesCount() < 12) {
            player.sendMessage("§6AMCEggVoid | §eБросайте нужные ресурсы внутрь портала! §7(Список и остаток указаны на голограмме)");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (altar.isPortalBlock(event.getBlock())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§6AMCEggVoid | §cНельзя ломать Алтарь Портала в Энд!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(altar::isPortalBlock);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(altar::isPortalBlock);
    }
}
