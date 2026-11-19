package ariolmc.online.aMCEggVoid.core.mail.gui;

import ariolmc.online.aMCEggVoid.core.mail.MailService;
import ariolmc.online.aMCEggVoid.core.mail.PlayerMail;
import io.neris.NGui.core.gui.button.ClickEventContext;
import io.neris.NGui.core.gui.button.controller.actions.ButtonAction;
import io.neris.NGui.core.services.MenuServiceController;
import io.neris.NGui.core.services.StaticMenuServiceController;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Действие NGui для кнопки «Забрать все предметы» из почты.
 */
public class ClaimAllMailAction implements ButtonAction {

    public static final String KEY = "amceggvoid_mail_claim_all";

    private final MailService mailService;
    private final MailMenuFactory menuFactory;

    public ClaimAllMailAction(MailService mailService, MailMenuFactory menuFactory) {
        this.mailService = mailService;
        this.menuFactory = menuFactory;
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String value() {
        return "all";
    }

    @Override
    public void execute(ClickEventContext click) {
        if (!(click.whoClicked instanceof Player player)) {
            return;
        }

        PlayerMail mail = mailService.getOrCreateMail(player.getName());
        int before = mail != null ? mail.size() : 0;
        int claimed = mailService.claimAll(player);
        int remaining = mail != null ? mail.size() : 0;

        if (claimed > 0) {
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.9f, 1.1f);
            if (remaining == 0) {
                player.sendMessage("§6AMCEggVoid | §aВы забрали все предметы с почты (§f" + before + "§a)!");
            } else {
                player.sendMessage("§6AMCEggVoid | §aЗабрано §f" + claimed + " §aпредметов. Осталось: §e" + remaining + "§a.");
            }
        } else if (before > 0) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
            player.sendMessage("§6AMCEggVoid | §cВ инвентаре нет места! На почте ждёт §e" + before + " §cпредмет(ов).");
        }

        MenuServiceController ngui = StaticMenuServiceController.getServiceController();
        ngui.getPlayerMenuOpener().open(player, menuFactory.create(new MailMenuContext(player.getUniqueId(), 0)));
    }
}
