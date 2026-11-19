package ariolmc.online.aMCEggVoid.core.mail.gui;

import ariolmc.online.aMCEggVoid.core.mail.MailService;
import io.neris.NGui.core.gui.button.ClickEventContext;
import io.neris.NGui.core.gui.button.MainItemEventContext;
import io.neris.NGui.core.gui.button.controller.actions.ButtonAction;
import io.neris.NGui.core.services.MenuServiceController;
import io.neris.NGui.core.services.StaticMenuServiceController;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Действие NGui для выдачи одного предмета из почты по индексу в NBT.
 */
public class ClaimMailItemAction implements ButtonAction {

    public static final String KEY = "amceggvoid_mail_claim_item";

    private final MailService mailService;
    private final MailMenuFactory menuFactory;
    private final int index;

    public ClaimMailItemAction(MailService mailService, MailMenuFactory menuFactory, int index) {
        this.mailService = mailService;
        this.menuFactory = menuFactory;
        this.index = index;
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String value() {
        return String.valueOf(index);
    }

    @Override
    public void execute(ClickEventContext click) {
        if (!(click.whoClicked instanceof Player player)) {
            return;
        }
        if (!(click instanceof MainItemEventContext main) || main.getMainItem() == null) {
            return;
        }

        MenuServiceController ngui = StaticMenuServiceController.getServiceController();
        String rawIndex = ngui.getNbtTagger().getValueStringNBT(main.getMainItem(), KEY);
        if (rawIndex == null) {
            return;
        }

        int itemIndex;
        try {
            itemIndex = Integer.parseInt(rawIndex);
        } catch (NumberFormatException ignored) {
            return;
        }

        MailService.ClaimSingleResult result = mailService.claimAt(player, itemIndex);
        switch (result) {
            case CLAIMED -> player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.9f, 1.15f);
            case PARTIAL -> {
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.9f, 1.0f);
                player.sendMessage("§6AMCEggVoid | §eЗабрана часть стака — в инвентаре недостаточно места.");
            }
            case NO_SPACE -> {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                player.sendMessage("§6AMCEggVoid | §cВ инвентаре нет свободного места!");
            }
            case EMPTY -> {
            }
        }

        int page = Math.max(0, itemIndex / MailMenuFactory.PAGE_SIZE);
        ngui.getPlayerMenuOpener().open(player, menuFactory.create(new MailMenuContext(player.getUniqueId(), page)));
    }
}
