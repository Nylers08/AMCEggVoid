package ariolmc.online.aMCEggVoid.core.mail.gui;

import ariolmc.online.aMCEggVoid.core.mail.MailService;
import ariolmc.online.aMCEggVoid.core.mail.PlayerMail;
import io.neris.NGui.core.gui.button.button.StaticNbtButton;
import io.neris.NGui.core.gui.button.controller.ButtonController;
import io.neris.NGui.core.gui.button.controller.SimpleBtnController;
import io.neris.NGui.core.gui.button.controller.actions.CancelPutAction;
import io.neris.NGui.core.gui.button.controller.renderers.ButtonRenderContext;
import io.neris.NGui.core.gui.button.controller.renderers.ButtonRenderer;
import io.neris.NGui.core.gui.menu.BaseMenu;
import io.neris.NGui.core.gui.menu.Menu;
import io.neris.NGui.core.gui.menu.services.MenuFactory;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Фабрика меню почты игрока на базе NGui.
 */
public class MailMenuFactory implements MenuFactory<MailMenuContext> {

    public static final int PAGE_SIZE = 45;
    private static final int MENU_SIZE = 54;

    private final MailService mailService;

    public MailMenuFactory(MailService mailService) {
        this.mailService = mailService;
    }

    @Override
    public String getMenuName() {
        return "amceggvoid_mail";
    }

    @Override
    public Menu create(MailMenuContext ctx) {
        UUID playerId = ctx.playerId();
        Player player = Bukkit.getPlayer(playerId);
        String playerName = player != null ? player.getName() : Bukkit.getOfflinePlayer(playerId).getName();
        PlayerMail mail = mailService.getOrCreateMail(playerName);

        List<ItemStack> items = mail != null ? mail.getItems() : List.of();
        int totalItems = items.size();
        int maxPage = totalItems == 0 ? 0 : (totalItems - 1) / PAGE_SIZE;
        int page = Math.max(0, Math.min(ctx.page(), maxPage));

        String title = totalItems == 0
                ? "§8Почта (пуста)"
                : "§8Почта (" + totalItems + " предм.) • Стр. " + (page + 1) + "/" + (maxPage + 1);
        Menu menu = new BaseMenu(MENU_SIZE, Component.text(title));

        if (totalItems == 0) {
            StaticNbtButton emptyInfo = new StaticNbtButton(
                    new SimpleBtnController(
                            Material.BARRIER,
                            1,
                            Component.text("§cВаша почта пуста"),
                            new CancelPutAction()
                    ),
                    new ButtonRenderContext(playerId)
            );
            menu.setItem(emptyInfo.getItem(), 22);
        } else {
            int start = page * PAGE_SIZE;
            int end = Math.min(start + PAGE_SIZE, totalItems);
            for (int i = start; i < end; i++) {
                int slot = i - start;
                ItemStack stored = items.get(i);
                menu.setItem(createMailItemButton(playerId, stored, i).getItem(), slot);
            }
        }

        StaticNbtButton filler = new StaticNbtButton(
                new SimpleBtnController(
                        Material.GRAY_STAINED_GLASS_PANE,
                        1,
                        Component.text(" "),
                        new CancelPutAction()
                ),
                new ButtonRenderContext(playerId)
        );
        for (int slot = 45; slot < MENU_SIZE; slot++) {
            menu.setItem(filler.getItem(), slot);
        }

        if (page > 0) {
            StaticNbtButton prevBtn = new StaticNbtButton(
                    new SimpleBtnController(
                            Material.ARROW,
                            1,
                            Component.text("§e← Предыдущая страница"),
                            new CancelPutAction(),
                            new MailPageAction(this, page - 1)
                    ),
                    new ButtonRenderContext(playerId)
            );
            menu.setItem(prevBtn.getItem(), 45);
        }

        if (totalItems > 0) {
            StaticNbtButton claimAllBtn = createClaimAllButton(playerId, totalItems);
            menu.setItem(claimAllBtn.getItem(), 49);
        }

        if (page < maxPage) {
            StaticNbtButton nextBtn = new StaticNbtButton(
                    new SimpleBtnController(
                            Material.ARROW,
                            1,
                            Component.text("§eСледующая страница →"),
                            new CancelPutAction(),
                            new MailPageAction(this, page + 1)
                    ),
                    new ButtonRenderContext(playerId)
            );
            menu.setItem(nextBtn.getItem(), 53);
        }

        return menu;
    }

    private StaticNbtButton createMailItemButton(UUID playerId, ItemStack stored, int index) {
        ButtonRenderer renderer = context -> {
            ItemStack copy = stored.clone();
            ItemMeta meta = copy.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.lore();
                List<Component> updatedLore = lore != null ? new ArrayList<>(lore) : new ArrayList<>();
                updatedLore.add(Component.empty());
                updatedLore.add(Component.text("§a▸ Нажмите, чтобы забрать"));
                meta.lore(updatedLore);
                copy.setItemMeta(meta);
            }
            return copy;
        };

        ButtonController controller = new ButtonController(renderer, new ArrayList<>());
        controller.addAction(new CancelPutAction());
        controller.addAction(new ClaimMailItemAction(mailService, this, index));
        return new StaticNbtButton(controller, new ButtonRenderContext(playerId));
    }

    private StaticNbtButton createClaimAllButton(UUID playerId, int totalItems) {
        ButtonRenderer renderer = context -> {
            ItemStack item = new ItemStack(Material.HOPPER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("§aЗабрать все предметы"));
                meta.lore(List.of(
                        Component.text("§7Предметов в почте: §e" + totalItems),
                        Component.text("§eНажмите, чтобы забрать всё")
                ));
                item.setItemMeta(meta);
            }
            return item;
        };

        ButtonController controller = new ButtonController(renderer, new ArrayList<>());
        controller.addAction(new CancelPutAction());
        controller.addAction(new ClaimAllMailAction(mailService, this));
        return new StaticNbtButton(controller, new ButtonRenderContext(playerId));
    }
}
